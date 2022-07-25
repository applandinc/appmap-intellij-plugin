package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class Languages {
    private static final List<Language> languages = loadLanguages();
    private static final Map<String, Language> extensionMapping = createExtensionMapping(languages);

    public static List<Language> getLanguages() {
        return languages;
    }

    public static @Nullable Language getLanguage(@NotNull String extension) {
        return extensionMapping.get(extension);
    }

    private static @NotNull Map<String, Language> createExtensionMapping(@NotNull List<Language> languages) {
        var mapping = new HashMap<String, Language>();
        for (var language : languages) {
            for (var extension : language.extensions) {
                mapping.put(StringUtil.trimStart(extension, "."), language);
            }
        }
        return mapping;
    }

    private static @NotNull List<Language> loadLanguages() {
        try (var resource = Languages.class.getResourceAsStream("/installGuide/languages.json")) {
            assert resource != null;

            return Arrays.asList(GsonUtils.GSON.fromJson(new InputStreamReader(resource), Language[].class));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Definition of a single language in the JSON data copied over from the vscode plugin, see language.json.
     */
    @Value
    public static class Language {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("extensions")
        public String[] extensions;
    }
}
