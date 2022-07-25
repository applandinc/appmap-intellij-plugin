package appland.installGuide.analyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class JavaLanguageAnalyzerTest extends InstallGuideBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void mavenPlain() {
        var result = loadDirectory("java-maven-plain");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertNull(result.getFeatures().web);
        assertNull(result.getFeatures().test);
    }

    @Test
    public void mavenSpring() {
        var result = loadDirectory("java-maven-spring");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertFramework(result.getFeatures().web, "Spring", Score.Good);
        assertNull(result.getFeatures().test);
    }

    @Test
    public void mavenSpringJUnit() {
        var result = loadDirectory("java-maven-spring-junit");
        assertJavaSettings(result.getFeatures(), "pom.xml", "com.appland:appmap-maven-plugin");
        assertFramework(result.getFeatures().web, "Spring", Score.Good);
        assertFramework(result.getFeatures().test, "JUnit", Score.Good);
    }

    private void assertJavaSettings(@NotNull Features features, @Nullable String depFile, @Nullable String pluginValue) {
        assertEquals("Java", features.lang.title);
        assertEquals(depFile, features.lang.depFile);
        assertEquals(pluginValue, features.lang.plugin);
        assertEquals("plugin", features.lang.pluginType);
        assertEquals(Score.Good, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }
}