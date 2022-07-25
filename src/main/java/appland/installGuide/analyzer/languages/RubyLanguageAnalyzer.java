package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RubyLanguageAnalyzer implements LanguageAnalyzer {
    private static final String GEMFILE = "Gemfile";

    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "ruby".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var gemfile = directory.findChild(GEMFILE);
        if (gemfile != null) {
            var wordScanner = new PatternWordScanner(gemfile);

            var features = new Features(createRubyBase(), null, null);
            features.lang.depFile = gemfile.getName();
            features.web = detectWebFramework(wordScanner);
            features.test = detectTestFramework(wordScanner);

            return new ProjectAnalysis(directory, features);
        }

        return new ProjectAnalysis(directory, new Features(createRubyFallback(), null, null));
    }

    private @NotNull FeatureEx createRubyBase() {
        var feature = new FeatureEx();
        feature.title = "Ruby";
        feature.score = Score.Good;
        feature.text = "This project looks like Ruby. It's one of the languages supported by AppMap.";
        return feature;
    }

    private @NotNull FeatureEx createRubyFallback() {
        var feature = new FeatureEx();
        feature.title = "Ruby";
        feature.score = Score.Okay;
        feature.text = "This project looks like Ruby. It's one of the languages supported by AppMap, but no Gemfile was detected.";
        return feature;
    }

    private @Nullable Feature detectWebFramework(@NotNull WordScanner wordScanner) {
        return StreamEx.of(WebFramework.values())
                .findFirst(v -> wordScanner.containsWord(v.markerWord))
                .map(WebFramework::createFeature)
                .orElse(null);
    }

    private @Nullable Feature detectTestFramework(@NotNull WordScanner wordScanner) {
        return StreamEx.of(TestFramework.values())
                .findFirst(v -> wordScanner.containsWord(v.markerWord))
                .map(TestFramework::createFeature)
                .orElse(null);
    }

    @AllArgsConstructor
    private enum WebFramework {
        Rails("Rails", "rails");

        private final String title;
        private final String markerWord;

        @NotNull Feature createFeature() {
            return new Feature(title,
                    Score.Good,
                    String.format("This project uses %s. AppMap will automatically recognize web requests, SQL queries, and key framework functions during recording.", title));
        }
    }

    @AllArgsConstructor
    private enum TestFramework {
        Minitest("minitest", "minitest"),
        RSpec("rspec", "rspec"),
        Cucumber("cucumber", "cucumber");

        private final String title;
        private final String markerWord;

        @NotNull Feature createFeature() {
            return new Feature(title,
                    Score.Good,
                    String.format("This project uses %s. Test execution can be automatically recorded.", title));
        }
    }
}
