package appland.problemsView;

import appland.Icons;
import appland.problemsView.model.ScannerFinding;
import com.intellij.analysis.problemsView.FileProblem;
import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ScannerFileProblem implements FileProblem {
    private final @NotNull Project project;
    private final @NotNull VirtualFile virtualFile;
    private final @NotNull ScannerFinding finding;

    public ScannerFileProblem(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull ScannerFinding finding) {
        this.project = project;
        this.virtualFile = virtualFile;
        this.finding = finding;
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return virtualFile;
    }

    @Override
    public int getLine() {
        var location = finding.getProblemLocation();
        return location == null || location.line == null ? -1 : location.line;
    }

    @Override
    public int getColumn() {
        return -1;
    }

    @Nullable
    @Override
    public String getDescription() {
        return finding.message;
    }

    @Nullable
    @Override
    public String getGroup() {
        return finding.groupMessage;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Icons.APPMAP_FILE;
    }

    @NotNull
    @Override
    public ProblemsProvider getProvider() {
        return problemsProvider;
    }

    @NotNull
    @Override
    public String getText() {
        return finding.message;
    }

    private final ProblemsProvider problemsProvider = new ProblemsProvider() {
        @Override
        public void dispose() {
        }

        @NotNull
        @Override
        public Project getProject() {
            return project;
        }
    };
}
