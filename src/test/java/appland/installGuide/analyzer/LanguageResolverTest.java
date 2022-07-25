package appland.installGuide.analyzer;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class LanguageResolverTest extends AppMapBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/language-resolver";
    }

    @Test
    public void javaProjectOneLevel() {
        assertLanguageResolver("java-oneLevel", "java");
    }

    @Test
    public void javaProject() {
        assertLanguageResolver("java-pure", "java");
    }

    @Test
    public void javaPython() {
        assertLanguageResolver("java-python", "java");
    }

    @Test
    public void pythonJava() {
        assertLanguageResolver("python-java", "python");
    }

    @Test
    public void javascriptOneLevel() {
        assertLanguageResolver("javascript-oneLevel", "javascript");
    }

    @Test
    public void javascriptPython() {
        assertLanguageResolver("javascript-python", "javascript");
    }

    private void assertLanguageResolver(@NotNull String directory, @Nullable String expectedLanguageId) {
        var root = myFixture.copyDirectoryToProject(directory, "root");

        var resolver = new LanguageResolver();
        var language = resolver.getLanguage(root);
        assertEquals(expectedLanguageId, language.id);
    }
}