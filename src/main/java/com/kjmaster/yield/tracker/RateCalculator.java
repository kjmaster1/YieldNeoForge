package com.kjmaster.yield.tracker;

import java.util.Arrays;

public class RateCalculator {
    // 60 buckets, one for each second in the minute window
    private final int[] buckets;
    private final int windowSeconds;

    private long lastSecTimestamp = 0;
    private int currentBucketIndex = 0;
    private int runningSum = 0;

    public RateCalculator(int windowSeconds) {
        this.windowSeconds = windowSeconds;
        this.buckets = new int[windowSeconds];
    }

    public void addGain(int amount) {
        if (amount <= 0) return;
        tick(); // Ensure time is synced
        buckets[currentBucketIndex] += amount;
        runningSum += amount;
    }

    // Call this before reading rates or adding gains
    private void tick() {
        long nowSec = System.currentTimeMillis() / 1000;
        if (lastSecTimestamp == 0) {
            lastSecTimestamp = nowSec;
            return;
        }

        long diff = nowSec - lastSecTimestamp;
        if (diff == 0) return; // Still in same second

        // Zero out buckets for the seconds that passed
        for (long i = 0; i < diff; i++) {
            currentBucketIndex = (currentBucketIndex + 1) % windowSeconds;
            // Subtract the old value leaving the window from the running sum
            runningSum -= buckets[currentBucketIndex];
            // Reset the bucket for the new second
            buckets[currentBucketIndex] = 0;

            // Optimization: If gap is huge (e.g. paused game), clear all
            if (i >= windowSeconds) {
                clear();
                lastSecTimestamp = nowSec;
                return;
            }
        }
        lastSecTimestamp = nowSec;
    }

    public double getItemsPerHour() {
        tick();
        // Prevent division by zero or unrealistic rates at start of session
        if (runningSum == 0) return 0.0;

        // Use max window for stability, or actual elapsed time if session just started
        return runningSum * (3600.0 / windowSeconds);
    }

    public void clear() {
        Arrays.fill(buckets, 0);
        runningSum = 0;
        currentBucketIndex = 0;
        lastSecTimestamp = 0;
    }
}