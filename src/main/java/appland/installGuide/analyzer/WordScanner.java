package appland.installGuide.analyzer;

import org.jetbrains.annotations.NotNull;

public interface WordScanner {
    boolean containsWord(@NotNull String word);

    com.intellij.openapi.vfs.VirtualFile getFile();
}
