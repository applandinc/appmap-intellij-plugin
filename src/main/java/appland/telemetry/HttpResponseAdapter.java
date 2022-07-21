package appland.telemetry;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Adapter between a Azure HTTP response and a JDK HTTP response.
 */
class HttpResponseAdapter extends HttpResponse {
    private final HttpHeaders responseHeaders = new HttpHeaders();
    private final int responseStatusCode;

    HttpResponseAdapter(@NotNull URLConnection connection, @NotNull HttpRequest request, int responseStatusCode) {
        super(request);
        this.responseStatusCode = responseStatusCode;

        for (var header : connection.getHeaderFields().entrySet()) {
            responseHeaders.set(header.getKey(), header.getValue());
        }
    }

    @Override
    public int getStatusCode() {
        return responseStatusCode;
    }

    @Override
    public String getHeaderValue(String name) {
        return responseHeaders.getValue(name);
    }

    @Override
    public HttpHeaders getHeaders() {
        return responseHeaders;
    }

    @Override
    public Flux<ByteBuffer> getBody() {
        // unsupported
        return Flux.empty();
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
        // unsupported
        return Mono.empty();
    }

    @Override
    public Mono<String> getBodyAsString() {
        // unsupported
        return Mono.empty();
    }

    @Override
    public Mono<String> getBodyAsString(Charset charset) {
        // unsupported
        return Mono.empty();
    }
}
