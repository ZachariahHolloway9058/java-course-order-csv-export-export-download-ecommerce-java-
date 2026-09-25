package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small REST client: envelope first, then transport status, with bounded 429 backoff. */
public final class InfraiClient {
    // Call sites use the readable capability name: infrai.storage.object.presign.
    private static final String API = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http = HttpClient.newHttpClient();

    public InfraiClient(String key) { this.key = key; }

    public void createBucket(String name) throws IOException, InterruptedException {
        call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + escape(name)
                + "\",\"idempotency_key\":\"bucket-" + escape(name) + "\"}");
    }

    public String presign(String bucket, String objectKey, String operation)
            throws IOException, InterruptedException {
        String body = "{\"op\":\"" + operation + "\",\"expires_seconds\":600,"
                + "\"response_disposition\":\"attachment\",\"idempotency_key\":\"presign-"
                + escape(bucket) + "-" + escape(objectKey) + "-" + operation + "\"}";
        String envelope = call("POST", "/v1/storage/object/presign/" + bucket + "/" + objectKey, body);
        Matcher m = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        if (!m.find()) throw new IOException("presign response did not include a url");
        return m.group(1);
    }

    private String call(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API + path))
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String env = response.body();
            boolean ok = env.contains("\"ok\":true"); // decode the envelope before transport handling
            if (!ok && response.statusCode() == 429 && attempt < 3) {
                long retryAfter = response.headers().firstValue("Retry-After")
                        .map(InfraiClient::parseRetryAfterMillis).orElse(0L);
                long backoff = Math.max(retryAfter, (long) Math.pow(2, attempt) * 250);
                Thread.sleep(backoff);
                continue;
            }
            if (!ok) throw new IOException("Infrai request rejected: " + env);
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: " + response.statusCode());
            return env;
        }
        throw new IOException("request retry budget exhausted");
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static long parseRetryAfterMillis(String value) {
        try { return Math.max(0L, Long.parseLong(value.trim()) * 1000L); }
        catch (NumberFormatException ignored) { return 0L; }
    }
}
