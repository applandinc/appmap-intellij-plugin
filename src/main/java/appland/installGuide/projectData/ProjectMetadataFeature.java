package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@AllArgsConstructor
public class ProjectMetadataFeature {
    @SerializedName("name")
    @Nullable String name;

    @SerializedName("score")
    int score;

    @SerializedName("text")
    @NotNull String text;
}
