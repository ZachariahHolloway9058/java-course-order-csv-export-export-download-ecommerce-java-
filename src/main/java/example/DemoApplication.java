package example;

import java.util.List;

public final class DemoApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY first");
        String bucket = "course-commerce-exports";
        InfraiClient client = new InfraiClient(key);
        client.createBucket(bucket);
        String download = new OrderExportService(client, bucket).exportPaidOrders(List.of(
                new Order("o-101", "Ada", "PAID", 4900), new Order("o-102", "Lin", "PENDING", 3200)));
        System.out.println("Download CSV: " + download);
    }
}
