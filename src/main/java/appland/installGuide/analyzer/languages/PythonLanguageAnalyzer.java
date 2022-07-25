package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class PythonLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "python".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var scanner = createWordScanner(directory);
        if (scanner != null) {
            var lang = createPythonDefault();
            lang.depFile = scanner.getFile().getName();

            var web = detectWebFramework(scanner);
            var test = detectTestFramework(directory, scanner);

            return new ProjectAnalysis(directory, new Features(lang, web, test));
        }

        return new ProjectAnalysis(directory, new Features(createPythonFallback(), null, null));
    }

    private @NotNull Feature detectWebFramework(@NotNull WordScanner scanner) {
        if (scanner.containsWord("django")) {
            return new Feature("Django",
                    Score.Okay,
                    "This project uses Django. AppMap will automatically recognize web requests, SQL queries, and key framework functions during recording.");
        }

        if (scanner.containsWord("flask")) {
            return new Feature("flask",
                    Score.Okay,
                    "Flask support is currently in Beta. Please read the docs.");
        }

        return new Feature(null, Score.Bad, "This project doesn't seem to use a supported web framework. Remote recording won't be possible.");
    }

    private @NotNull Feature detectTestFramework(VirtualFile directory, WordScanner scanner) {
        if (scanner.containsWord("pytest")) {
            return new Feature("pytest",
                    Score.Okay,
                    "This project uses pytest. Test execution can be automatically recorded.");
        }

        if (grepPythonFiles(directory, "unittest")) {
            return new Feature("unittest",
                    Score.Okay,
                    "This project uses unittest. Test execution can be automatically recorded.");
        }

        return new Feature(null,
                Score.Bad,
                "This project doesn't seem to use a supported test framework. Automatic test recording won't be possible.");
    }

    private @Nullable WordScanner createWordScanner(@NotNull VirtualFile directory) {
        var requirements = directory.findChild("requirements.txt");
        if (requirements != null) {
            return new PatternWordScanner(requirements, word -> {
                var regex = "^" + Pattern.quote(word) + "(\\W|$)";
                return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            });
        }

        var pyProject = directory.findChild("pyproject.toml");
        if (pyProject != null) {
            return new PatternWordScanner(pyProject);
        }

        return null;
    }

    private @NotNull FeatureEx createPythonDefault() {
        var feature = new FeatureEx();
        feature.title = "Python";
        feature.score = Score.Okay;
        feature.text = "Python is currently in Open Beta and is not fully supported. Please read the docs before proceeding.";
        return feature;
    }

    private @NotNull FeatureEx createPythonFallback() {
        var feature = new FeatureEx();
        feature.title = "Python";
        feature.score = Score.Okay;
        feature.text = "This looks like a Python project without a package manager. Python is currently in Open Beta and is not fully supported. Please read the docs before proceeding.";
        return feature;
    }

    private static boolean grepPythonFiles(@NotNull VirtualFile directory, @NotNull String substring) {
        var found = new AtomicBoolean(false);

        VfsUtilCore.iterateChildrenRecursively(directory,
                file -> file.isDirectory() || file.getName().endsWith(".py"),
                file -> {
                    if (file.isDirectory()) {
                        return true;
                    }

                    try {
                        var document = FileDocumentManager.getInstance().getDocument(file);
                        var content = document != null ? document.getText() : VfsUtilCore.loadText(file);
                        if (content.contains(substring)) {
                            found.set(true);
                        }
                    } catch (IOException e) {
                        found.set(false);
                    }

                    return found.get();
                }, VirtualFileVisitor.limit(10));

        return found.get();
    }
}
