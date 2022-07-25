package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Feature {
    @SerializedName("title")
    public @Nullable String title;

    @SerializedName("score")
    public @NotNull Score score;

    @SerializedName("text")
    public @NotNull String text;
}
