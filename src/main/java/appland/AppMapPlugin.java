package appland;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
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

    public static Path getInstallGuideHTMLPath() {
        return getPluginPath().resolve("appland-install-guide").resolve("index.html");
    }

    @NotNull
    public static PluginDescriptor getDescriptor() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert plugin != null;
        return plugin;
    }
}
