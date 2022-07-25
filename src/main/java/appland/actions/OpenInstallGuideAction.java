package appland.actions;

import appland.installGuide.InstallGuideEditorProvider;
import appland.installGuide.InstallGuideViewPage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OpenInstallGuideAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(InstallGuideEditorProvider.isSupported());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        InstallGuideEditorProvider.open(Objects.requireNonNull(e.getProject()), InstallGuideViewPage.InstallAgent);
    }
}
