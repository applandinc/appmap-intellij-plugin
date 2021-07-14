package appland.lang.agents;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AppMapRubyAgent implements AppMapAgent {
    private static final int TIMEOUT_MILLIS = (int) TimeUnit.MINUTES.toMillis(5);
    private static final String GEM_DEPENDENCY = "\ngem 'appmap', :groups => [:development, :test]";
    private static final Pattern PATTERN_GEM_DECLARATION = Pattern.compile("(?m)^\\s*gem\\s+");
    private static final Pattern PATTERN_GEM_DEPENDENCY = Pattern.compile("(?m)^\\s*gem\\s+['|\"]appmap['|\"].*$");

    @Override
    public @NotNull AgentLanguage getLanguage() {
        return AgentLanguage.Ruby;
    }

    @Override
    public List<VirtualFile> detectRoots(@NotNull Project project) {
        var result = new ArrayList<VirtualFile>();

        for (var module : ModuleManager.getInstance(project).getModules()) {
            var roots = ModuleRootManager.getInstance(module).getContentRoots();
            for (var root : roots) {
                if (isGitDir(root) && findGemfile(root) != null) {
                    result.add(root);
                }
            }
        }

        return result;
    }

    @Override
    @RequiresBackgroundThread
    public boolean isInstalled(@NotNull VirtualFile root) throws ExecutionException {
        assert root.isDirectory();
        return execute(root, "bundle", "info", "appmap").getExitCode() == 0;
    }

    @Override
    @RequiresBackgroundThread
    public @NotNull InstallResult install(@NotNull VirtualFile root) throws ExecutionException {
        assert root.isDirectory();

        var gemfile = findGemfile(root);
        if (gemfile == null) {
            return InstallResult.None;
        }

        var document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(gemfile));
        var newContent = updateGemDependency(document.getImmutableCharSequence());
        WriteAction.runAndWait(() -> {
            document.setText(newContent);
            FileDocumentManager.getInstance().saveDocument(document);
        });

        var isInstalled = isInstalled(root);
        if (isInstalled) {
            var output = execute(root, "bundle", "update", "appmap");
            if (output.getExitCode() != 0) {
                throw new ExecutionException("Failed to execute 'bundle update appmap': " + output);
            }
        } else {
            var output = execute(root, "bundle", "install");
            if (output.getExitCode() != 0) {
                throw new ExecutionException("Failed to execute 'bundle install': " + output);
            }
        }

        return InstallResult.Installed;
    }

    @Override
    @RequiresBackgroundThread
    public boolean init(@NotNull VirtualFile root) throws ExecutionException {
        assert root.isDirectory();

        var output = execute(root, "bundle", "exec", "appmap-agent-init");
        if (output.getExitCode() != 0) {
            throw new ExecutionException("Failed to execute 'bundle exec appmap-agent-init'");
        }

        var response = AgentJson.parseInitResponse(output.getStdout());
        if (response == null || !response.isValid()) {
            throw new ExecutionException("Failed to parse JSON init response: " + output);
        }

        try {
            WriteAction.runAndWait(() -> {
                var file = root.createChildData(this, response.filename);
                var doc = FileDocumentManager.getInstance().getDocument(file);
                if (doc == null) {
                    throw new IOException("Failed to retrieve document for file " + file.getPath());
                }

                doc.setText(response.contents);
            });
        } catch (IOException e) {
            throw new ExecutionException("Failed to create file " + response.filename, e);
        }

        return false;
    }

    @NotNull
    private ProcessOutput execute(@NotNull VirtualFile root, @NotNull @NonNls String... command) throws ExecutionException {
        var cmd = new GeneralCommandLine(command);
        cmd.setWorkDirectory(VfsUtil.virtualToIoFile(root));
        return ExecUtil.execAndGetOutput(cmd, TIMEOUT_MILLIS);
    }

    @NotNull
    static CharSequence updateGemDependency(@NotNull CharSequence content) {

        var gemDependencyMatcher = PATTERN_GEM_DECLARATION.matcher(content);
        var gemBlockFound = gemDependencyMatcher.find();
        if (gemBlockFound) {
            var matcher = PATTERN_GEM_DEPENDENCY.matcher(content);
            var hasExistingDependency = matcher.find();
            if (hasExistingDependency) {
                content = matcher.replaceFirst("\n" + GEM_DEPENDENCY);
            } else {
                var index = gemDependencyMatcher.start();
                content = content.subSequence(0, index) + GEM_DEPENDENCY + "\n\n" + content.subSequence(index, content.length());
            }
        } else {
            // append AppMap dependency at the end of the file
            content = content + "\n" + GEM_DEPENDENCY;
        }
        return content;
    }

    static @Nullable VirtualFile findGemfile(@NotNull VirtualFile parentDir) {
        return parentDir.findChild("Gemfile");
    }

    static boolean isGitDir(@NotNull VirtualFile parentDir) {
        var child = parentDir.findChild(".git");
        return child != null && child.isDirectory();
    }
}
