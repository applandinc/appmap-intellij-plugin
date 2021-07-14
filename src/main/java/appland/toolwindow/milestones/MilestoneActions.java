package appland.toolwindow.milestones;

import appland.lang.agents.AppMapAgent;
import appland.lang.agents.InstallResult;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

class MilestoneActions {
    private static final Logger LOG = Logger.getInstance("#appland.milestones");

    static void installAppMapAgent(@NotNull Project project) {
        var task = new Task.Backgroundable(project, "Detecting projects...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                AppMapAgent.detectLanguages(project).forEach((language, roots) -> {
                    var agent = AppMapAgent.findByLanguage(language);
                    assert agent != null;

                    for (VirtualFile root : roots) {
                        indicator.setText(String.format("Installing %s agent in %s...", language.getName(), root.getPath()));
                        LOG.warn("Installing agent in root " + root.getPath() + " for language " + language.getName());
                        try {
                            var isInstalled = agent.isInstalled(root);
                            if (!isInstalled) {
                                var result = agent.install(root);
                                if (result != InstallResult.Installed) {
                                    LOG.warn("failed to install agent in root " + root.getPath());
                                    continue;
                                }
                            }
                        } catch (ExecutionException e) {
                            LOG.error(e);
                            continue;
                        }

                        try {
                            agent.init(root);
                        } catch (ExecutionException e) {
                            LOG.error(e);
                        }
                    }
                });
            }
        };

        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
    }
}
