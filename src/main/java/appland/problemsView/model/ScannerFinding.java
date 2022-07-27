package appland.problemsView.model;

import appland.files.FileLocation;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ScannerFinding {
    @SerializedName("hash")
    public @NotNull String hash = "";

    @SerializedName("ruleId")
    public @NotNull String ruleId = "";

    @SerializedName("ruleTitle")
    public @NotNull String ruleTitle = "";

    @SerializedName("stack")
    public @NotNull List<String> stack = Collections.emptyList();

    @SerializedName("message")
    public @NotNull String message = "";

    @SerializedName("groupMessage")
    public @NotNull String groupMessage = "";

    public @NotNull String getFindingTitle() {
        return ruleTitle +
                ": " +
                (StringUtil.isNotEmpty(groupMessage) ? groupMessage : StringUtil.defaultIfEmpty(message, ""));
    }

    public @Nullable String getDescription() {
        if (StringUtil.isNotEmpty(message)) {
            return message;
        }
        if (StringUtil.isNotEmpty(groupMessage)) {
            return groupMessage;
        }
        return null;
    }

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
