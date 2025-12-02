package com.kjmaster.yield.util;

import com.kjmaster.yield.Yield;
import java.util.concurrent.*;

public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Yield-Debouncer");
        t.setDaemon(false);
        return t;
    });

    private ScheduledFuture<?> future;
    private Runnable lastTask;

    public Debouncer() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
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
                    // Only clear if this specific task finished (simple check)
                    if (this.lastTask == task) {
                        this.lastTask = null;
                    }
                }
            }
        }, delay, unit);
    }

    private void onShutdown() {
        // Run pending task immediately on the shutdown thread
        Runnable pending;
        synchronized (this) {
            pending = lastTask;
            lastTask = null; // Prevent double execution
        }

        if (pending != null) {
            Yield.LOGGER.info("Yield is shutting down, forcing pending save...");
            try {
                pending.run();
            } catch (Exception e) {
                Yield.LOGGER.error("Failed to execute pending save on shutdown", e);
            }
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                Yield.LOGGER.warn("Yield Debouncer did not terminate in time, forcing shutdown.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}