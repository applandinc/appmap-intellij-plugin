package appland.lang.agents;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class AgentJson {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOG = Logger.getInstance("#appmap.agent");

    @Nullable
    static AgentInitResponse parseInitResponse(@NotNull String json) {
        try {
            var data = GSON.fromJson(json, JsonObject.class);
            var configuration = data.getAsJsonObject("configuration");
            return GSON.fromJson(configuration, AgentInitResponse.class);
        } catch (JsonSyntaxException e) {
            LOG.debug("Syntax error parsing agent init response: " + json, e);
            return null;
        }
    }
}
