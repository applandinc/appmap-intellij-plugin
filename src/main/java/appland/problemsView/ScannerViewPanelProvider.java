package appland.problemsView;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewState;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScannerViewPanelProvider implements ProblemsViewPanelProvider {
    private final Project project;

    public ScannerViewPanelProvider(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ProblemsViewTab create() {
        return new ScannerProblemsViewTab(project, ProblemsViewState.getInstance(project));
    }
}
