package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "java".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        for (var buildSystem : BuildSystem.values()) {
            var file = directory.findChild(buildSystem.filename);
            if (file != null) {
                var features = new Features(createJavaBase(), null, null);
                features.lang.depFile = buildSystem.filename;
                buildSystem.applyTo(features.lang);

                var wordScanner = new PatternWordScanner(file);
                features.web = detectWebFramework(wordScanner);
                features.test = detectTestFramework(wordScanner);

                return new ProjectAnalysis(directory, features);
            }
        }

        return new ProjectAnalysis(directory, new Features(createLanguageFallback(), null, null));
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
        Spring("Spring", "spring");

        private final String title;
        private final String markerWord;

        @NotNull Feature createFeature() {
            return new Feature(title,
                    Score.Good,
                    String.format("This project uses %s. AppMap enables recording web requests and remote recording.", title));
        }
    }

    @AllArgsConstructor
    private enum TestFramework {
        JUnit("JUnit", "junit"),
        TestNG("TestNG", "testng");

        private final String title;
        private final String markerWord;

        @NotNull Feature createFeature() {
            return new Feature(title,
                    Score.Good,
                    String.format("This project uses %s. Test execution can be automatically recorded.", title));
        }
    }

    @AllArgsConstructor
    private enum BuildSystem {
        Maven("pom.xml", "com.appland:appmap-maven-plugin"),
        Gradle("build.gradle", "com.appland.appmap");

        private final String filename;
        private final String plugin;

        void applyTo(@NotNull FeatureEx feature) {
            feature.plugin = plugin;
            feature.pluginType = "plugin";
        }
    }

    private @NotNull FeatureEx createJavaBase() {
        var feature = new FeatureEx();
        feature.title = "Java";
        feature.score = Score.Good;
        feature.text = "This project looks like Java. It's one of languages supported by AppMap!";
        return feature;
    }

    private @NotNull FeatureEx createLanguageFallback() {
        var feature = new FeatureEx();
        feature.title = "Java";
        feature.score = Score.Okay;
        feature.text = "This project looks like Java, but we couldn't find a supported dependency file.";
        return feature;
    }
}
