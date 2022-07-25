package appland.installGuide.analyzer;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class ProjectAnalysis {
    @SerializedName("score")
    int score;

    @SerializedName("name")
    String name;

    @SerializedName("path")
    String path;

    @SerializedName("features")
    Features features;

    public ProjectAnalysis(@NotNull VirtualFile file, @NotNull Features features) {
        this(file.getName(), mapToNativePath(file), features);
    }

    public ProjectAnalysis(@NotNull String name, @NotNull String filePath, @NotNull Features features) {
        this.name = name;
        this.path = filePath;
        this.features = features;

        this.score = features.getTotalScore();
    }

    private static @NotNull String mapToNativePath(@NotNull VirtualFile file) {
        var nioPath = file.getFileSystem().getNioPath(file);
        return nioPath != null ? nioPath.toString() : FileUtilRt.toSystemDependentName(file.getPath());
    }
}
