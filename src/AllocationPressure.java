import java.util.ArrayList;
import java.util.List;

/** Creates short-lived data repeatedly; stop with Ctrl+C. */
public class AllocationPressure {
    private static volatile long sink;
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Allocation pressure running. Stop with Ctrl+C.");
        while (true) {
            createTemporaryObjects();
            Thread.sleep(50);
        }
    }
    static void createTemporaryObjects() {
        List<byte[]> batch = new ArrayList<>();
        for (int i = 0; i < 1000; i++) batch.add(new byte[4096]);
        sink = batch.size(); // Batch becomes unreachable after this method returns.
    }
}
