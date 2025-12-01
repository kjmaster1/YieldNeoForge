package com.kjmaster.yield.util;

import java.util.concurrent.*;

public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Yield-Debouncer");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> future;

    public void debounce(Runnable task, long delay, TimeUnit unit) {
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        future = scheduler.schedule(task, delay, unit);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}