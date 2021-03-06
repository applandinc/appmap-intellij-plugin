package appland.startup;

import appland.milestones.MilestonesViewType;
import appland.milestones.UserMilestonesEditorProvider;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import org.jetbrains.annotations.NotNull;

public class AppLandStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        if (AppMapApplicationSettingsService.getInstance().isFirstStart() && !unitTestMode) {
            AppMapApplicationSettingsService.getInstance().setFirstStart(false);

            openToolWindowAndQuickstart(project);
        }
    }

    static void openToolWindowAndQuickstart(@NotNull Project project) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
            UserMilestonesEditorProvider.open(project, MilestonesViewType.InstallAgent);
        });
    }
}
