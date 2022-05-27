package appland.problemsView.model;

import appland.files.FileLocation;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ScannerFinding {
    @SerializedName("hash")
    public @NotNull String hash = "";

    @SerializedName("stack")
    public @NotNull List<String> stack = Collections.emptyList();

    @SerializedName("message")
    public @NotNull String message = "";

    @SerializedName("groupMessage")
    public @NotNull String groupMessage = "";

    public @Nullable FileLocation getProblemLocation() {
        if (stack.isEmpty()) {
            return null;
        }

        var candidate = stack.stream().filter(path -> !path.startsWith("/")).findFirst().orElse(null);
        if (candidate == null) {
            return null;
        }

        return FileLocation.parse(candidate);
    }
}
