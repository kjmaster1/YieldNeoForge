package com.kjmaster.yield.tracker;

import java.util.ArrayDeque;
import java.util.Deque;

public class RateCalculator {
    private record GainEntry(long timestamp, int amount) {}

    private final Deque<GainEntry> history = new ArrayDeque<>();
    private final long windowMillis;
    private int currentSum = 0; // Optimization: Running sum

    public RateCalculator(int windowSeconds) {
        this.windowMillis = windowSeconds * 1000L;
    }

    public void addGain(int amount) {
        if (amount <= 0) return;
        long now = System.currentTimeMillis();
        history.addLast(new GainEntry(now, amount));
        currentSum += amount; // O(1) Add
        prune(now);
    }

    private void prune(long now) {
        long cutoff = now - windowMillis;
        // O(k) where k is the number of expired entries (usually 0 or 1 per tick)
        while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
            currentSum -= history.removeFirst().amount;
        }
    }

    public double getItemsPerHour() {
        long now = System.currentTimeMillis();
        prune(now); // Ensure data is strictly within the window

        if (history.isEmpty()) return 0.0;

        // Calculate actual time span for accurate "Instant Rate" at start of session
        long oldestTime = history.peekFirst().timestamp;
        double effectiveSeconds = Math.max(1000, now - oldestTime) / 1000.0;

        return (currentSum / effectiveSeconds) * 3600.0;
    }

    public void clear() {
        history.clear();
        currentSum = 0;
    }
}