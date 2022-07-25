package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaScriptLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "javascript".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var packageJson = directory.findChild("package.json");

        Features features = null;
        if (packageJson != null) {
            features = findPackageJsonFeatures(packageJson);
        }

        if (features == null) {
            features = new Features(createLanguageFallback(), null, null);
        }

        return new ProjectAnalysis(directory, features);
    }

    private @Nullable Features findPackageJsonFeatures(@NotNull VirtualFile file) {
        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return null;
        }

        var json = GsonUtils.GSON.fromJson(document.getText(), JsonObject.class);

        FeatureEx web = null;
        if (GsonUtils.hasProperty(json, "dependencies", "express")) {
            web = new FeatureEx();
            web.title = "express.js";
            web.score = Score.Good;
            web.text = "This project uses express.js. AppMap enables recording web requests and remote recording.";
        }

        FeatureEx test = null;
        var mocha = GsonUtils.getPath(json, "devDependencies", "mocha");
        if (mocha != null && mocha.isString()) {
            test = new FeatureEx();
            test.title = "mocha";

            if (isAtLeastMocha8(mocha.getAsString())) {
                test.score = Score.Good;
                test.text = "This project uses mocha. Test execution can be automatically recorded.";
            } else {
                test.score = Score.Bad;
                test.text = "This project uses mocha, but the version is too old. You need at least version 8 to automatically record test execution.";
            }
        }

        var lang = new FeatureEx("package.json", "@appland/appmap-agent-js", "package");
        lang.title = "JavaScript";
        lang.score = Score.Good;
        lang.text = "This project looks like JavaScript. It's one of languages supported by AppMap!";
        return new Features(lang, web, test);
    }

    private boolean isAtLeastMocha8(@NotNull String value) {
        var version = SemVer.parseFromText(StringUtil.trimStart(StringUtil.trimStart(value, "^"), "~"));
        return version != null && version.isGreaterOrEqualThan(8, 0, 0);
    }

    private @NotNull FeatureEx createLanguageFallback() {
        var feature = new FeatureEx();
        feature.title = "JavaScript";
        feature.score = Score.Okay;
        feature.text = "This project looks like JavaScript, but we couldn't read a supported dependency file.";
        return feature;
    }
}
