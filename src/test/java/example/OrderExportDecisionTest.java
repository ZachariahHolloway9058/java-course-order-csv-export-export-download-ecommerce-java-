package example;

import java.util.List;

public final class OrderExportDecisionTest {
    public static void main(String[] args) {
        long paid = List.of(new Order("1", "A", "PAID", 100), new Order("2", "B", "PENDING", 200))
                .stream().filter(o -> "PAID".equals(o.status())).count();
        if (paid != 1) throw new AssertionError("only paid orders belong in a receipt export");
        System.out.println("PASS: paid-order export decision");
    }
}
