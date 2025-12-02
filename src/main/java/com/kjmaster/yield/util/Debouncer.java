package com.kjmaster.yield.util;

import com.kjmaster.yield.Yield;
import java.util.concurrent.*;

public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Yield-Debouncer");
        t.setDaemon(false); // Changed to false so it can finish tasks on shutdown if needed
        return t;
    });

    private ScheduledFuture<?> future;
    private Runnable lastTask;

    public Debouncer() {
        // Ensure pending saves are written if the game closes unexpectedly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (lastTask != null) {
                Yield.LOGGER.info("Yield is shutting down, forcing pending save...");
                lastTask.run();
            }
            scheduler.shutdownNow();
        }));
    }

    public synchronized void debounce(Runnable task, long delay, TimeUnit unit) {
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        this.lastTask = task;
        future = scheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                synchronized (this) {
                    lastTask = null; // Clear task after execution
                }
            }
        }, delay, unit);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}