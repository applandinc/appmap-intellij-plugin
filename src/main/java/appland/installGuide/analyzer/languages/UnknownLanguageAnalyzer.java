package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Fallback language analyzer, must be configured with order=last in plugin.xml.
 */
public class UnknownLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return true;
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var lang = new FeatureEx();
        lang.score = Score.Bad;
        lang.text = "This project looks like it's written in a language not currently supported by AppMap.";

        return new ProjectAnalysis(directory, new Features(lang, null, null));
    }
}
