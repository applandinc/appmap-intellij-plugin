package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public class Features {
    @SerializedName("language")
    public @NotNull FeatureEx lang;

    @SerializedName("webFramework")
    public @Nullable Feature web;

    @SerializedName("testFramework")
    public @Nullable Feature test;

    int getTotalScore() {
        return lang.score.getScoreValue()
                + (web == null ? 0 : web.score.getScoreValue())
                + (test == null ? 0 : test.score.getScoreValue());
    }
}