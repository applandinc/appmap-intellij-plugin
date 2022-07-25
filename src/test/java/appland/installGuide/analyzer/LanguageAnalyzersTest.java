package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LanguageAnalyzersTest extends AppMapBaseTest {
    @Test
    public void analyzers() {
        assertAnalyzer("java");
        assertAnalyzer("js");
    }

    private void assertAnalyzer(@NotNull String fileExtension) {
        var language = Languages.getLanguage(fileExtension);
        assertNotNull(language);
        assertNotNull(LanguageAnalyzer.create(language));
    }
}