package com.kjmaster.yield.tracker;

import java.util.Arrays;

public class RateCalculator {
    private final int[] buckets;
    private final int windowSeconds;

    private long lastSecTimestamp = 0;
    private long startTime = 0; // Track start for accurate initial rates
    private int currentBucketIndex = 0;
    private int runningSum = 0;

    public RateCalculator(int windowSeconds) {
        this.windowSeconds = windowSeconds;
        this.buckets = new int[windowSeconds];
    }

    public void addGain(int amount) {
        if (amount <= 0) return;
        tick();
        buckets[currentBucketIndex] += amount;
        runningSum += amount;
    }

    private void tick() {
        long nowSec = System.currentTimeMillis() / 1000;
        if (lastSecTimestamp == 0) {
            lastSecTimestamp = nowSec;
            startTime = nowSec;
            return;
        }

        long diff = nowSec - lastSecTimestamp;
        if (diff == 0) return;

        // Optimization: If the gap exceeds the window, the rate is effectively 0.
        // Reset everything immediately without looping.
        if (diff >= windowSeconds) {
            clear(); // Resets buckets and runningSum
            // Keep startTime to track session duration accurately,
            // but reset lastSecTimestamp to now.
            lastSecTimestamp = nowSec;
            return;
        }

        // Small gap: Perform the rolling window update
        for (int i = 0; i < diff; i++) {
            currentBucketIndex = (currentBucketIndex + 1) % windowSeconds;
            runningSum -= buckets[currentBucketIndex];
            buckets[currentBucketIndex] = 0;
        }
        lastSecTimestamp = nowSec;
    }

    public double getItemsPerHour() {
        tick();
        if (runningSum == 0) return 0.0;

        long nowSec = System.currentTimeMillis() / 1000;
        // Calculate actual time active since last reset/start
        long activeDuration = nowSec - startTime;

        // Use the smaller of: Window Size OR Actual Time Passed
        // This fixes the "Slow Start" issue.
        double divisor = Math.min(windowSeconds, Math.max(1, activeDuration));

        return runningSum * (3600.0 / divisor);
    }

    public void clear() {
        Arrays.fill(buckets, 0);
        runningSum = 0;
        currentBucketIndex = 0;
        lastSecTimestamp = 0;
        startTime = 0;
    }
}