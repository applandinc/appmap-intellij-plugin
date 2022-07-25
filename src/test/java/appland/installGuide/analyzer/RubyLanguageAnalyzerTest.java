package appland.installGuide.analyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class RubyLanguageAnalyzerTest extends InstallGuideBaseTest {
    @Override
    protected String getBasePath() {
        return "installGuide/sample-projects";
    }

    @Test
    public void railsRSpec() {
        var result = loadDirectory("ruby-rails-rspec");
        assertLanguage(result.getFeatures(), "Gemfile", null);
        assertWeb(result.getFeatures(), "Rails", Score.Good);
        assertTest(result.getFeatures(), "rspec", Score.Good);
    }

    private void assertLanguage(@NotNull Features features, @Nullable String depFile, @Nullable String pluginValue) {
        assertEquals("Ruby", features.lang.title);
        assertEquals(depFile, features.lang.depFile);
        assertEquals(pluginValue, features.lang.plugin);
        assertEquals(Score.Good, features.lang.score);
        assertFalse(features.lang.text.isEmpty());
    }
}