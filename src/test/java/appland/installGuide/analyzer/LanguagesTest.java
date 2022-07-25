package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class LanguagesTest extends AppMapBaseTest {
    @Test
    public void languages() {
        var languages = Languages.getLanguages();
        assertNotEmpty(languages);

        assertLanguage("java", "java");
        assertLanguage("js", "javascript");
    }

    private void assertLanguage(@NotNull String fileExtension, @NotNull String expectedLanguageId) {
        var language = Languages.getLanguage(fileExtension);
        assertNotNull(language);
        assertEquals(expectedLanguageId, language.id);
    }
}