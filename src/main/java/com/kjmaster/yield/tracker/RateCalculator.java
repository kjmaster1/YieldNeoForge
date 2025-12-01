package com.kjmaster.yield.tracker;

import java.util.ArrayDeque;
import java.util.Deque;

public class RateCalculator {
    private record GainEntry(long timestamp, int amount) {}

    private final Deque<GainEntry> history = new ArrayDeque<>();
    private final long windowMillis;

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

        // 1. Prune old entries (Standard Rolling Window)
        while (!history.isEmpty() && history.peekFirst().timestamp < cutoff) {
            history.removeFirst();
        }

        if (history.isEmpty()) return 0.0;

        // 2. Sum gains
        int totalGains = history.stream().mapToInt(GainEntry::amount).sum();

        // 3. Calculate "Effective Window"
        // Instead of always dividing by 60s, we divide by how much time
        // has actually passed since the oldest relevant data point.
        // This makes the rate "Instant" at the start and "Smoothed" after 60s.
        long oldestTime = history.peekFirst().timestamp;
        long timeSinceFirstGain = now - oldestTime;

        // Clamp minimum divisor to 1 second (1000ms) to prevent divide-by-zero or infinite spikes
        double effectiveSeconds = Math.max(1000, timeSinceFirstGain) / 1000.0;

        // 4. Calculate Hourly Rate
        return (totalGains / effectiveSeconds) * 3600.0;
    }

    public void clear() {
        history.clear();
    }
}