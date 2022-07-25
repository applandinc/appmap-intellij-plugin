package appland.installGuide.analyzer;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.regex.Pattern;

public class PatternWordScanner implements WordScanner {
    @Getter
    private final @NotNull VirtualFile file;
    private final Function<String, Pattern> patternProvider;
    private final String fileContent;

    public PatternWordScanner(@NotNull VirtualFile file) {
        this(file, PatternWordScanner::createDefaultPattern);
    }

    public PatternWordScanner(@NotNull VirtualFile file, @NotNull Function<String, Pattern> patternProvider) {
        this.file = file;
        this.patternProvider = patternProvider;

        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            fileContent = document.getText();
        } else {
            fileContent = "";
        }
    }

    @Override
    public boolean containsWord(@NotNull String word) {
        var pattern = patternProvider.apply(word);
        return pattern.matcher(fileContent).find();
    }

    @NotNull
    private static Pattern createDefaultPattern(@NotNull String word) {
        return Pattern.compile("(\\W|^)" + Pattern.quote(word) + "(\\W|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }
}
