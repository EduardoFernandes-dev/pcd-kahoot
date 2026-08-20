package coordination;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
public class CustomBarrier {
    private final int parties;
    private int count;
    private final Runnable barrierAction;
    private boolean broken;
    private int generation;
    private boolean actionExecuted;
    public CustomBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException("Parties must be positive");
        }
        this.parties = parties;
        this.count = parties;
        this.barrierAction = barrierAction;
        this.broken = false;
        this.generation = 0;
        this.actionExecuted = false;
    }
    public synchronized int await() throws InterruptedException {
        if (broken) {
            throw new IllegalStateException("Barrier is broken");
        }
        int arrivalIndex = count;
        count--;
        if (count == 0) {
            Runnable actionToRun = barrierAction;
            nextGeneration();
            notifyAll();
            if (actionToRun != null) {
                executeActionOutsideLock(actionToRun);
            }
            return 1;
        }
        int myGeneration = generation;
        try {
            while (count > 0 && myGeneration == generation && !broken) {
                wait();
            }
        } catch (InterruptedException e) {
            count++;
            notifyAll();
            throw e;
        }
        if (broken) {
            throw new IllegalStateException("Barrier broken during wait");
        }
        return arrivalIndex;
    }
    public synchronized int await(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        if (broken) {
            throw new IllegalStateException("Barrier is broken");
        }
        long millis = unit.toMillis(timeout);
        if (millis <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        int arrivalIndex = count;
        count--;
        if (count == 0) {
            Runnable actionToRun = barrierAction;
            nextGeneration();
            notifyAll();
            if (actionToRun != null) {
                executeActionOutsideLock(actionToRun);
            }
            return 1;
        }
        int myGeneration = generation;
        long deadline = System.currentTimeMillis() + millis;
        long remaining = millis;
        try {
            while (count > 0 && myGeneration == generation && !broken && remaining > 0) {
                wait(remaining);
                remaining = deadline - System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            count++;
            notifyAll();
            throw e;
        }
        if (count > 0 && myGeneration == generation) {
            if (!actionExecuted) {
                actionExecuted = true;
                Runnable actionToRun = barrierAction;
                nextGeneration();
                notifyAll();
                if (actionToRun != null) {
                    executeActionOutsideLock(actionToRun);
                }
            }
            throw new TimeoutException("Barrier timeout");
        }
        if (broken) {
            throw new IllegalStateException("Barrier broken during wait");
        }
        return arrivalIndex;
    }
    public synchronized void reset() {
        if (getNumberWaiting() > 0) {
            throw new IllegalStateException("Cannot reset barrier while threads are waiting");
        }
        count = parties;
        broken = false;
        actionExecuted = false;
        generation++;
        notifyAll();
    }
    public synchronized void reduceParties() {
        if (count > 0) {
            count--;
            if (count == 0 && !actionExecuted) {
                actionExecuted = true;
                Runnable actionToRun = barrierAction;
                nextGeneration();
                notifyAll();
                if (actionToRun != null) {
                    executeActionOutsideLock(actionToRun);
                }
            }
        }
    }
    public synchronized boolean isBroken() {
        return broken;
    }
    public synchronized int getNumberWaiting() {
        return parties - count;
    }
    public synchronized int getParties() {
        return parties;
    }
    private void nextGeneration() {
        count = parties;
        actionExecuted = false;
        generation++;
    }
    private void executeActionOutsideLock(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            System.err.println("Barrier action threw exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
