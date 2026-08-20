package coordination;
public class CustomSemaphore {
    private int licenses;
    private final int initialLicenses;
    private int acquireCount;
    private static final int BONUS_LIMIT = 2;
    private boolean timeoutReached;
    private int generation;
    public CustomSemaphore(int licenses) {
        if (licenses <= 0) {
            throw new IllegalArgumentException("Licenses must be positive");
        }
        this.licenses = licenses;
        this.initialLicenses = licenses;
        this.acquireCount = 0;
        this.timeoutReached = false;
        this.generation = 0;
    }
    public synchronized int acquire() throws InterruptedException {
        int myGeneration = generation;
        while (licenses <= 0 && !timeoutReached && myGeneration == generation) {
            wait();
        }
        if (licenses > 0 && myGeneration == generation) {
            licenses--;
            acquireCount++;
            int multiplier = (acquireCount <= BONUS_LIMIT) ? 2 : 1;
            notifyAll();
            return multiplier;
        }
        return 0;
    }
    public synchronized boolean waitForTimeout(long millis) throws InterruptedException {
        if (millis <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        long deadline = System.currentTimeMillis() + millis;
        long remaining = millis;
        while (licenses > 0 && remaining > 0) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        if (licenses > 0) {
            timeoutReached = true;
            notifyAll();
            return false;
        }
        return true;
    }
    public synchronized void reset() {
        licenses = initialLicenses;
        acquireCount = 0;
        timeoutReached = false;
        generation++;
        notifyAll();
    }
    public synchronized int getAcquireCount() {
        return acquireCount;
    }
    public synchronized boolean isTimeoutReached() {
        return timeoutReached;
    }
    public synchronized int getRemainingLicenses() {
        return licenses;
    }
}
