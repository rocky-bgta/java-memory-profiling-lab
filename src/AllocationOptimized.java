/** Reuses a bounded buffer instead of allocating a batch on every iteration. */
public class AllocationOptimized {
    private static volatile long sink;
    public static void main(String[] args) throws InterruptedException {
        byte[] buffer = new byte[4096];
        System.out.println("Reusable buffer running. Stop with Ctrl+C.");
        while (true) {
            processWithReusableBuffer(buffer);
            Thread.sleep(50);
        }
    }
    static void processWithReusableBuffer(byte[] buffer) {
        long sum = 0;
        for (int i = 0; i < 1000; i++) {
            buffer[0] = (byte) i;
            sum += buffer[0];
        }
        sink = sum;
    }
}
