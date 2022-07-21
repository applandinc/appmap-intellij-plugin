package appland.telemetry;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImpl;
import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImplBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.HttpURLConnection;
import java.util.List;

/* Based on:
https://github.com/Azure/azure-sdk-for-java/blob/4a53adf6274ced5af8243983042b5e32bac85bd7/sdk/monitor/azure-monitor-opentelemetry-exporter/src/main/java/com/azure/monitor/opentelemetry/exporter/AzureMonitorExporterBuilder.java
*/
class AppInsightsClient {
    private final @NotNull String instrumentationKey;
    private final @NotNull ApplicationInsightsClientImpl client;

    AppInsightsClient(@NotNull String connectionString) {
        var builder = new ApplicationInsightsClientImplBuilder();
        builder.httpClient(new JdkHttpClient());

        String key = null;
        for (String setting : connectionString.split(";")) {
            String[] kv = setting.split("=");
            if (kv.length != 2)
                continue;

            switch (kv[0]) {
                case "InstrumentationKey":
                    key = kv[1];
                    break;
                case "IngestionEndpoint":
                    builder.host(kv[1]);
                    break;
            }
        }

        instrumentationKey = key == null ? "" : key;
        client = builder.buildClient();
    }

    public void track(@NotNull TelemetryItem item) {
        item.setInstrumentationKey(instrumentationKey);

        /* TODO: add context (session and user id) */
        client.trackAsync(List.of(item));
    }

    /**
     * Adapter to use the JDK's HTTP client to handle HTTP communication with the Azure servers.
     */
    private static class JdkHttpClient implements HttpClient {
        @Override
        public Mono<HttpResponse> send(HttpRequest request) {
            var contentTypeHeader = request.getHeaders().getValue("content-type");

            var url = request.getUrl().toExternalForm();
            var contentType = contentTypeHeader != null ? contentTypeHeader : "application/json";
            var body = request.getBody().blockFirst();
            var method = request.getHttpMethod();

            RequestBuilder httpRequest;
            switch (method) {
                case GET:
                    httpRequest = HttpRequests.request(url);
                    break;
                case POST:
                    httpRequest = HttpRequests.post(url, contentType);
                    break;
                case DELETE:
                    httpRequest = HttpRequests.delete(url, contentType);
                    break;
                default:
                    throw new RuntimeException("Unsupported HTTP method: " + method);
            }

            httpRequest.tuner(connection -> {
                for (HttpHeader header : request.getHeaders()) {
                    connection.setRequestProperty(header.getName(), header.getValue());
                }
            });

            httpRequest.throwStatusCodeException(false);

            return Mono.create(sink -> {
                try {
                    httpRequest.connect(newRequest -> {
                        if (body != null && body.hasRemaining()) {
                            newRequest.write(body.array());
                        }

                        var connection = newRequest.getConnection();
                        var statusCode = connection instanceof HttpURLConnection
                                ? ((HttpURLConnection) connection).getResponseCode()
                                : -1;

                        sink.success(new HttpResponseAdapter(connection, request, statusCode));

                        return statusCode;
                    });
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        }
    }
}