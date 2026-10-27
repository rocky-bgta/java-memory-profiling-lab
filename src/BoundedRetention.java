import java.util.ArrayDeque;
import java.util.Deque;

/** Keeps only the latest ten entries, bounding retained data. */
public class BoundedRetention {
    private static final Deque<byte[]> CACHE = new ArrayDeque<>();
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Keeping at most 10 MiB. Stop with Ctrl+C.");
        while (true) {
            retainOnlyRecentData();
            Thread.sleep(500);
        }
    }
    static void retainOnlyRecentData() {
        if (CACHE.size() == 10) CACHE.removeFirst();
        CACHE.addLast(new byte[1024 * 1024]);
    }
}
