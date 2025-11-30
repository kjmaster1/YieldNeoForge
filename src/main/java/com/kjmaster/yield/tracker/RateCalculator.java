package com.kjmaster.yield.tracker;

import java.util.ArrayDeque;
import java.util.Deque;

public class RateCalculator {
    // A record of a gain: how many items, and when (in system millis)
    private record GainEntry(long timestamp, int amount) {}

    private final Deque<GainEntry> history = new ArrayDeque<>();
    private final long windowMillis;

    /**
     * @param windowSeconds The time window to calculate the average over (e.g. 60 seconds)
     */
    public RateCalculator(int windowSeconds) {
        this.windowMillis = windowSeconds * 1000L;
    }

    public void addGain(int amount) {
        if (amount <= 0) return;
        history.addLast(new GainEntry(System.currentTimeMillis(), amount));
    }

    public double getItemsPerHour() {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis;

        // Remove old entries
        while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
            history.removeFirst();
        }

        if (history.isEmpty()) return 0.0;

        // Sum up all gains in the window
        int totalGainsInWindow = history.stream().mapToInt(GainEntry::amount).sum();

        // Math: (Gains / WindowSeconds) * 3600
        // We use the full window size for stability, rather than time since first entry
        double windowSeconds = windowMillis / 1000.0;
        return (totalGainsInWindow / windowSeconds) * 3600.0;
    }

    public void clear() {
        history.clear();
    }
}
