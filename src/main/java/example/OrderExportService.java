package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class OrderExportService {
    private final InfraiClient infrai;
    private final String bucket;
    private final HttpClient http = HttpClient.newHttpClient();

    public OrderExportService(InfraiClient infrai, String bucket) { this.infrai = infrai; this.bucket = bucket; }

    public String exportPaidOrders(List<Order> orders) throws Exception {
        StringBuilder csv = new StringBuilder("order_id,customer,status,total_cents\n");
        orders.stream().filter(o -> "PAID".equals(o.status()))
                .forEach(o -> csv.append(o.id()).append(',').append(o.customer()).append(',')
                        .append(o.status()).append(',').append(o.cents()).append('\n'));
        String key = "exports/orders-" + System.currentTimeMillis() + ".csv";
        String uploadUrl = infrai.presign(bucket, key, "put");
        HttpRequest upload = HttpRequest.newBuilder(URI.create(uploadUrl)).PUT(
                HttpRequest.BodyPublishers.ofString(csv.toString(), StandardCharsets.UTF_8))
                .header("Content-Type", "text/csv").build();
        HttpResponse<Void> uploaded = http.send(upload, HttpResponse.BodyHandlers.discarding());
        if (uploaded.statusCode() / 100 != 2) throw new IOException("CSV upload failed: " + uploaded.statusCode());
        return infrai.presign(bucket, key, "get");
    }
}
