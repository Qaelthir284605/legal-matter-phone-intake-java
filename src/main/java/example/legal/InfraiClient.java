package example.legal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String apiKey;

    public InfraiClient(ObjectMapper json, @Value("${infrai.base-url}") String baseUrl,
                        @Value("${infrai.api-key}") String apiKey) {
        this.json = json;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public JsonNode post(String path, Map<String, ?> body) {
        try {
            String payload = json.writeValueAsString(body);
            for (int attempt = 0; attempt < 3; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 2) {
                    long seconds = response.headers().firstValue("Retry-After")
                        .flatMap(value -> { try { return java.util.Optional.of(Long.parseLong(value)); }
                                           catch (NumberFormatException e) { return java.util.Optional.empty(); } })
                        .orElse(1L << attempt);
                    Thread.sleep(Math.min(8, Math.max(1, seconds)) * 1000);
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    throw new Rejection(response.statusCode(), envelope.path("error"));
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Upstream request failed");
                return envelope.path("data");
            }
            throw new IllegalStateException("Retry budget exhausted");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read upstream response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        }
    }

    public static class Rejection extends RuntimeException {
        private final int status;
        private final JsonNode error;
        public Rejection(int status, JsonNode error) {
            super(error.path("message").asText("Request rejected"));
            this.status = status;
            this.error = error;
        }
        public int status() { return status; }
        public JsonNode error() { return error; }
    }
}
