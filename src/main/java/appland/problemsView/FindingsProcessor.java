package appland.problemsView;

import appland.problemsView.model.FindingsFileData;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface FindingsProcessor {
    void process(@NotNull VirtualFile findingsFile, @NotNull FindingsFileData findingsData);
}
