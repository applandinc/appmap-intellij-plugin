package appland.projectPicker;

import appland.AppMapBaseTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class LanguageResolverTest extends AppMapBaseTest {
    @Override
    protected String getBasePath() {
        return "projectPicker/language-resolver";
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

    private void assertLanguageResolver(@NotNull String directory, @Nullable String expectedLanguageId) {
        var root = myFixture.copyDirectoryToProject(directory, "root");

        var resolver = new LanguageResolver();
        var language = resolver.getLanguage(root);
        assertEquals(expectedLanguageId, language);
    }
}