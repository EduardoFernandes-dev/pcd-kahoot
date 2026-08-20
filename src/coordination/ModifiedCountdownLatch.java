package coordination;
public class ModifiedCountdownLatch {
    private int count;
    private final int initialCount;
    private final int bonusFactor;
    private final int bonusCount;
    private final int waitPeriod; 
    private int countdownsReceived;
    private boolean timeoutReached;
    public ModifiedCountdownLatch(int bonusFactor, int bonusCount, int waitPeriod, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        if (waitPeriod <= 0) {
            throw new IllegalArgumentException("Wait period must be positive");
        }
        if (bonusFactor < 1) {
            throw new IllegalArgumentException("Bonus factor must be at least 1");
        }
        if (bonusCount < 0) {
            throw new IllegalArgumentException("Bonus count cannot be negative");
        }
        this.count = count;
        this.initialCount = count;
        this.bonusFactor = bonusFactor;
        this.bonusCount = bonusCount;
        this.waitPeriod = waitPeriod;
        this.countdownsReceived = 0;
        this.timeoutReached = false;
    }
    public synchronized int countdown() {
        if (count <= 0 || timeoutReached) {
            return 0; 
        }
        count--;
        countdownsReceived++;
        int multiplier;
        if (countdownsReceived <= bonusCount) {
            multiplier = bonusFactor;
        } else {
            multiplier = 1;
        }
        if (count == 0) {
            notifyAll();
        }
        return multiplier;
    }
    public synchronized void await() throws InterruptedException {
        if (count <= 0) {
            return; 
        }
        long deadline = System.currentTimeMillis() + waitPeriod;
        long remaining = waitPeriod;
        while (count > 0 && remaining > 0 && !timeoutReached) {
            wait(remaining);
            remaining = deadline - System.currentTimeMillis();
        }
        if (count > 0) {
            timeoutReached = true;
            notifyAll();
        }
    }
    public synchronized void reset() {
        count = initialCount;
        countdownsReceived = 0;
        timeoutReached = false;
        notifyAll();
    }
    public synchronized void reduceCount() {
        if (count > 0) {
            count--;
            if (count == 0) {
                notifyAll();
            }
        }
    }
    public synchronized int getCount() {
        return count;
    }
    public synchronized boolean isTimeoutReached() {
        return timeoutReached;
    }
    public synchronized int getCountdownsReceived() {
        return countdownsReceived;
    }
}
