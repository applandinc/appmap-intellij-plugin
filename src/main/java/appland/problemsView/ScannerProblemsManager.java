package appland.problemsView;

import appland.files.FileLookup;
import appland.problemsView.model.FindingsFileData;
import appland.problemsView.model.ScannerFinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.SlowOperations;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// fixme(jansorg): This is a slow implementation to collect feedback
public class ScannerProblemsManager {
    private static final Logger LOG = Logger.getInstance(ScannerProblemsManager.class);
    private static final Gson GSON = new GsonBuilder().create();

    private final @NotNull Project project;

    public static @NotNull ScannerProblemsManager getInstance(@NotNull Project project) {
        return project.getService(ScannerProblemsManager.class);
    }

    public ScannerProblemsManager(@NotNull Project project) {
        this.project = project;
    }

    public Map<VirtualFile, List<ScannerFinding>> processFindings() {
        var result = new HashMap<VirtualFile, List<ScannerFinding>>();
        var unknownFileResults = new ArrayList<ScannerFinding>();

        SlowOperations.allowSlowOperations(() -> processAllFindings((findingsFile, findings) -> {
            var parentDir = findingsFile.getParent();

            for (ScannerFinding finding : findings.findings) {
                var location = finding.getProblemLocation();
                if (location != null) {
                    var problemFile = FileLookup.findRelativeFile(project, parentDir, location.filePath);
                    if (problemFile != null) {
                        result.computeIfAbsent(problemFile, virtualFile -> new ArrayList<>()).add(finding);
                        continue;
                    }
                }

                // fallback
                unknownFileResults.add(finding);
            }
        }));

        if (unknownFileResults.size() > 0) {
            var file = new LightVirtualFile("Unknown Files");
            result.put(file, unknownFileResults);
        }

        return result;
    }

    public void processAllFindings(@NotNull FindingsProcessor processor) {
        for (VirtualFile file : findFindingsFiles()) {
            var document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                try {
                    processor.process(file, GSON.fromJson(document.getText(), FindingsFileData.class));
                } catch (JsonSyntaxException e) {
                    LOG.warn("Error parsing findings: " + file.getPath(), e);
                }
            }
        }
    }

    public @NotNull Collection<VirtualFile> findFindingsFiles() {
        var scope = GlobalSearchScope.projectScope(project);
        return FilenameIndex.getVirtualFilesByName(project, "appmap-findings.json", true, scope);
    }
}
