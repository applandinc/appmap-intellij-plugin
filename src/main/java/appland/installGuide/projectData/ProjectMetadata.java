package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
public class ProjectMetadata {
    @SerializedName("name")
    @NotNull String name;

    @SerializedName("path")
    @NotNull String path;

    @SerializedName("score")
    int score;

    @SerializedName("hasNode")
    boolean hasNode;

    @SerializedName("agentInstalled")
    boolean agentInstalled;

    @SerializedName("appMapsRecorded")
    boolean appMapsRecorded;

    @SerializedName("analysisPerformed")
    boolean analysisPerformed;

    @SerializedName("appMapOpened")
    boolean appMapOpened;

    @SerializedName("numFindings")
    @Nullable Integer numFindings;

    @SerializedName("language")
    @Nullable ProjectMetadataFeature language;

    @SerializedName("testFramework")
    @Nullable ProjectMetadataFeature testFramework;

    @SerializedName("webFramework")
    @Nullable ProjectMetadataFeature webFramework;

//    @SerializedName("appMaps")
//    @Nullable List<AppMapSummary> appMaps;
}
