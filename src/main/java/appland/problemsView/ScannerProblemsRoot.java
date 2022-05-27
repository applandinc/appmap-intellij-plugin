package appland.problemsView;

import appland.problemsView.model.ScannerFinding;
import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.Root;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ScannerProblemsRoot extends Root {
    private final Map<VirtualFile, List<ScannerFinding>> data;

    public ScannerProblemsRoot(@NotNull ProblemsViewPanel panel) {
        super(panel);
        this.data = ScannerProblemsManager.getInstance(getProject()).processFindings();
    }

    @Override
    public int getFileProblemCount(@NotNull VirtualFile virtualFile) {
        return data.getOrDefault(virtualFile, Collections.emptyList()).size();
    }

    @NotNull
    @Override
    public Collection<Problem> getFileProblems(@NotNull VirtualFile virtualFile) {
        var findings = data.get(virtualFile);
        if (findings == null || findings.isEmpty()) {
            return Collections.emptyList();
        }

        return findings.stream()
                .map(scannerFinding -> new ScannerFileProblem(getProject(), virtualFile, scannerFinding))
                .collect(Collectors.toList());
    }

    @Override
    public int getOtherProblemCount() {
        return 0;
    }

    @NotNull
    @Override
    public Collection<Problem> getOtherProblems() {
        return List.of();
    }

    @Override
    public int getProblemCount() {
        return data.size();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getProblemFiles() {
        return data.keySet();
    }
}
