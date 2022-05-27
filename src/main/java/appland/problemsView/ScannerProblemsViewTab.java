package appland.problemsView;

import appland.AppMapBundle;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ScannerProblemsViewTab extends ProblemsViewPanel implements FileEditorManagerListener {
    public ScannerProblemsViewTab(@NotNull Project project, @NotNull ProblemsViewState state) {
        super(project, "appmap.scannerView", state, () -> AppMapBundle.get("scanner.problemsView.tabName"));

        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);

        refreshRoot();
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        refreshRoot();
    }

    @Override
    protected void updateToolWindowContent() {
        super.updateToolWindowContent();
    }

    private void refreshRoot() {
        getTreeModel().setRoot(new ScannerProblemsRoot(this));
    }
}
