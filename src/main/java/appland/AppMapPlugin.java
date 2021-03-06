package appland;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class AppMapPlugin {
    public static final String REMOTE_RECORDING_HELP_URL = "https://appland.com/docs/reference/remote-recording";

    private static final String PLUGIN_ID = "appland.appmap";

    private AppMapPlugin() {
    }

    @NotNull
    public static Path getPluginPath() {
        var plugin = getDescriptor();
        var basePath = plugin.getPluginPath();
        assert basePath != null;

        return basePath;
    }

    public static Path getAppMapHTMLPath() {
        return getPluginPath().resolve("appmap").resolve("index.html");
    }

    public static Path getInstallAgentHTMLPath() {
        return getPluginPath().resolve("appland-user-milestones").resolve("agent.html");
    }

    public static Path getAppMapsTableHTMLPath() {
        return getPluginPath().resolve("appland-user-milestones").resolve("appmaps.html");
    }

    public static Path getRecordAppMapsHTMLPath() {
        return getPluginPath().resolve("appland-user-milestones").resolve("record.html");
    }

    @NotNull
    public static PluginDescriptor getDescriptor() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert plugin != null;
        return plugin;
    }

    @NotNull
    public static String getProjectDirPath(Project project) {
        var projectDirName = "";
        var projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            var nioProjectDir = projectDir.getFileSystem().getNioPath(projectDir);
            if (nioProjectDir != null) {
                projectDirName = nioProjectDir.toString();
                if (projectDirName.indexOf(' ') > -1) projectDirName = "\"" + projectDirName + "\"";
            }
        }
        return projectDirName;
    }
}
