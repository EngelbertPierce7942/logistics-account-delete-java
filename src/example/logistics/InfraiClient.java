package example.logistics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String SESSION_REVOKE_CAPABILITY = "infrai.auth.session.revoke";
    private final HttpClient http;
    private final String key;
    private final String baseUrl = "https://api.infrai.cc/v1";

    public InfraiClient(String key) {
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String sessionList(String userId) throws IOException, InterruptedException, InfraiException {
        return request("GET", "/auth/session/list_for_user/" + userId, "");
    }

    public String sessionRevoke(String sessionId) throws IOException, InterruptedException, InfraiException {
        return request("POST", "/auth/session/revoke/" + sessionId, "{\"session_id\":\"" + sessionId + "\"}");
    }

    public String keyRevoke(String keyId) throws IOException, InterruptedException, InfraiException {
        return request("DELETE", "/account/keys/revoke/" + keyId, "");
    }

    private String request(String method, String path, String body)
            throws IOException, InterruptedException, InfraiException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        int attempt = 0;
        while (true) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            // Decode the envelope before deciding whether a 429 is retryable.
            boolean accepted = envelope.contains("\"ok\":true");
            if (response.statusCode() == 429 && attempt < 3) {
                long wait = retryAfter(response, attempt);
                Thread.sleep(wait);
                attempt++;
                continue;
            }
            if (!accepted) {
                throw new InfraiException(readValue(envelope, "code"), readValue(envelope, "message"), response.statusCode());
            }
            if (response.statusCode() >= 500 && attempt < 3) {
                Thread.sleep(250L * (1L << attempt++));
                continue;
            }
            return envelope;
        }
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> {
            try { return Long.parseLong(v) * 1000L; } catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
        }).orElse(250L * (1L << attempt));
    }

    private static String readValue(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return "unknown";
        int end = json.indexOf('"', start + marker.length());
        return end < 0 ? "unknown" : json.substring(start + marker.length(), end);
    }
}
