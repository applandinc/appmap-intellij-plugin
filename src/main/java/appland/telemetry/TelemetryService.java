package appland.telemetry;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class TelemetryService {
    private final AppInsightsClient client = new AppInsightsClient("KEY");

    /**
     * Sends data to the telemetry server.
     * The request is sent in a background thread.
     *
     * @param name Name of the tracked event
     */
    public void track(@NotNull String name) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var item = new EventTelemetryBuilder(name).build();
            client.track(item);
        });
    }
}
