package appland.installGuide.projectData;

import appland.installGuide.analyzer.*;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.CapturingProcessRunner;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DefaultProjectDataService implements ProjectDataService {
    private static final Logger LOG = Logger.getInstance(DefaultProjectDataService.class);
    private static final IntSet supportedNodeMajorVersions = IntSet.of(14, 16, 18);

    private final Project project;
    private final AtomicReference<@NotNull List<@NotNull ProjectMetadata>> cachedProjects = new AtomicReference<>(List.of());

    public DefaultProjectDataService(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull List<ProjectMetadata> getAppMapProjects() {
        updateMetadata();
        return cachedProjects.get();
    }

    private void updateMetadata() {
        var updatedProjects = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            return ReadAction.compute(() -> {
                return scanContentRoots()
                        .entrySet()
                        .stream()
                        .map(this::createMetadata)
                        .collect(Collectors.toList());
            });
        }, "Scanning project", true, project);

        if (updatedProjects != null) {
            cachedProjects.set(updatedProjects);
        }
    }

    private @NotNull ProjectMetadata createMetadata(@NotNull Map.Entry<VirtualFile, ProjectAnalysis> pair) {
        var root = pair.getKey();
        var analysis = pair.getValue();

        var progressIndicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        assert progressIndicator != null;
        var nodeVersion = findNodeVersion(progressIndicator, root);

        // support fallback to non-NIO path for our tests
        var nioPath = root.getFileSystem().getNioPath(root);
        var path = nioPath != null ? nioPath.toString() : root.getPath();

        return ProjectMetadata.builder()
                .name(root.getPresentableName())
                .path(path)
                .score(analysis.getScore())
                .hasNode(isNodeSupported(nodeVersion))
                .agentInstalled(isAgentInstalled(root))
                .appMapsRecorded(hasRecordedAppMaps(root))
                .analysisPerformed(isAnalysisPerformed())
                .appMapOpened(isAppMapOpened(root))
                .numFindings(getNumFindings())
                .language(mapLanguageMetadata(analysis.getFeatures().getLang()))
                .testFramework(mapLanguageMetadata(analysis.getFeatures().getTest()))
                .webFramework(mapLanguageMetadata(analysis.getFeatures().getWeb()))
                //.appmaps(analysis.appmaps)
                .build();
    }

    private @Nullable ProjectMetadataFeature mapLanguageMetadata(@Nullable Feature feature) {
        if (feature == null) {
            return null;
        }

        return new ProjectMetadataFeature(feature.getTitle(), feature.getScore().getScoreValue() + 1, feature.getText());
    }

    private @Nullable Integer getNumFindings() {
        return null;
    }

    private boolean isAnalysisPerformed() {
        return false;
    }

    private boolean isAppMapOpened(@NotNull VirtualFile root) {
        return false;
    }

    private boolean hasRecordedAppMaps(@NotNull VirtualFile root) {
        return false;
    }

    private boolean isAgentInstalled(@NotNull VirtualFile root) {
        return false;
    }

    private boolean isNodeSupported(@Nullable NodeVersion nodeVersion) {
        return nodeVersion != null
                && nodeVersion.getMajor() != 0
                && supportedNodeMajorVersions.contains(nodeVersion.getMajor());
    }

    private Map<VirtualFile, ProjectAnalysis> scanContentRoots() {
        var projects = new HashMap<VirtualFile, ProjectAnalysis>();
        var resolver = new LanguageResolver();

        for (var root : findTopLevelContentRoots()) {
            var language = resolver.getLanguage(root);
            if (language != null) {
                var analyzer = LanguageAnalyzer.create(language);
                if (analyzer != null) {
                    projects.put(root, analyzer.analyze(root));
                }
            }
        }
        return projects;
    }

    private @NotNull VirtualFile[] findTopLevelContentRoots() {
        var roots = new ArrayList<>(List.of(ProjectRootManager.getInstance(project).getContentRoots()));
        roots.sort(Comparator.comparingInt(o -> o.getPath().length()));

        var visited = new HashSet<VirtualFile>();
        for (var iterator = roots.iterator(); iterator.hasNext(); ) {
            var root = iterator.next();
            if (VfsUtil.isUnder(root, visited)) {
                iterator.remove();
            } else {
                visited.add(root);
            }
        }

        return roots.toArray(VirtualFile.EMPTY_ARRAY);
    }

    private @Nullable NodeVersion findNodeVersion(@NotNull ProgressIndicator indicator, @NotNull VirtualFile rootFolder) {
        // nvm version
        var nvmFile = rootFolder.findChild(".nvmrc");
        if (nvmFile != null) {
            var nodeVersion = parseNvmFile(nvmFile);
            if (nodeVersion != null) {
                return nodeVersion;
            }
        }

        // system node version
        var nodeExecutable = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("node");
        if (nodeExecutable != null) {
            return parseStdoutNodeVersion(indicator, nodeExecutable);
        }

        // null as default
        return null;
    }

    @Nullable
    private static NodeVersion parseNvmFile(VirtualFile nvmFile) {
        try {
            var text = VfsUtilCore.loadText(nvmFile);
            var nodeVersion = NodeVersion.parse(text);
            if (nodeVersion != null) {
                return nodeVersion;
            }
        } catch (IOException e) {
            LOG.debug("Error parsing .nvmrc file, path: " + nvmFile.getPath(), e);
        }
        return null;
    }

    @Nullable
    private static NodeVersion parseStdoutNodeVersion(@NotNull ProgressIndicator indicator, @NotNull File node) {
        try {
            var cmdline = new GeneralCommandLine(node.getPath(), "-v");
            var process = new CapturingProcessRunner(new KillableProcessHandler(cmdline));
            var output = process.runProcess(indicator, (int) TimeUnit.SECONDS.toMillis(1), true);
            if (output.getExitCode() == 0) {
                var nodeVersion = NodeVersion.parse(output.getStdout());
                if (nodeVersion != null) {
                    return nodeVersion;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to launch node -v, node: " + node, e);
        }
        return null;
    }
}
