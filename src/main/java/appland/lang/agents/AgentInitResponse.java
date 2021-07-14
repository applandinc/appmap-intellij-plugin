package appland.lang.agents;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * JSON response returned by an agent's init command.
 */
final class AgentInitResponse {
    @SerializedName("filename")
    @NotNull
    public String filename;

    @SerializedName("contents")
    @NotNull
    public String contents;

    public boolean isValid() {
        return filename != null && !filename.isEmpty();
    }
}
