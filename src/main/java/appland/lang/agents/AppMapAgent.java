package appland.lang.agents;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is an extension point.
 */
public interface AppMapAgent {
    Logger LOG = Logger.getInstance("#appmap.agent");

    ExtensionPointName<AppMapAgent> EP = ExtensionPointName.create("appland.agent");

    @Nullable
    static AppMapAgent findByLanguage(@NotNull AgentLanguage language) {
        return EP.findFirstSafe(appMapAgent -> language.equals(appMapAgent.getLanguage()));
    }

    /**
     * Detects all available agents in the given project. It iterates all content roots of all modules.
     *
     * @param project The project to inspect
     * @return A mapping of language to roots available for this language's agent.
     */
    @NotNull
    @RequiresBackgroundThread
    static Map<AgentLanguage, List<VirtualFile>> detectLanguages(@NotNull Project project) {
        var result = new HashMap<AgentLanguage, List<VirtualFile>>();

        EP.forEachExtensionSafe(agent -> {
            var roots = agent.detectRoots(project);
            if (!roots.isEmpty()) {
                result.put(agent.getLanguage(), roots);
            }
        });

        return result;
    }

    /**
     * @return The language supported by this agent.
     */
    @NotNull
    AgentLanguage getLanguage();

    /**
     * Detects possible root directories where this agent could be installed and initialized.
     *
     * @param project The project to inspect
     */
    List<VirtualFile> detectRoots(@NotNull Project project);

    /**
     * Execute the agent CLI to find out whether it's installed.
     * This method must be called in a background thread.
     *
     * @param root The root directory where the agent is configured. This has to be the path to a directory, not a file.
     * @return {@code true} if the agent is installed in the given root directory.
     * @throws ExecutionException Thrown when execution failed with an unexpected error.
     */
    @RequiresBackgroundThread
    boolean isInstalled(@NotNull VirtualFile root) throws ExecutionException;

    /**
     * Installs the agent into the given directory.
     * The implementation depends on the agent and language.
     * It's possible that files are modified and that a CLI tool is executed.
     * <p>
     * This method must be called in a background thread.
     *
     * @param root The root directory
     * @return The result of the installation process
     */
    @NotNull
    @RequiresBackgroundThread
    InstallResult install(@NotNull VirtualFile root) throws ExecutionException;

    @RequiresBackgroundThread
    boolean init(@NotNull VirtualFile root) throws ExecutionException;

//    @NotNull
//    FilesResponse files(@NotNull VirtualFile root);

//    @NotNull
//    StatusResponse status(@NotNull VirtualFile root);
}
