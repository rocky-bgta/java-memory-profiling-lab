import java.util.ArrayList;
import java.util.List;

/** Simulates retention with a bounded list, not an uncontrolled OOM. */
public class RetainedObjects {
    private static final List<byte[]> CACHE = new ArrayList<>();
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Retaining up to 96 MiB, then holding it. Stop with Ctrl+C.");
        while (true) {
            retainData();
            Thread.sleep(500);
        }
    }
    static void retainData() {
        if (CACHE.size() < 96) {
            CACHE.add(new byte[1024 * 1024]);
            System.out.println("Retained MiB: " + CACHE.size());
        }
    }
}
