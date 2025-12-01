package com.kjmaster.yield.tracker;

import com.kjmaster.yield.time.TimeSource;

import java.util.Arrays;

public class RateCalculator {
    private final int[] buckets;
    private final int windowSeconds;
    private final TimeSource timeSource;

    private long lastSecTimestamp = 0;
    private double startTime = 0;
    private int currentBucketIndex = 0;
    private int runningSum = 0;

    public RateCalculator(int windowSeconds, TimeSource timeSource) {
        this.windowSeconds = windowSeconds;
        this.buckets = new int[windowSeconds];
        this.timeSource = timeSource;
    }

    public void addGain(int amount) {
        if (amount <= 0) return;
        tick();
        buckets[currentBucketIndex] += amount;
        runningSum += amount;
    }

    private void tick() {
        long nowSec = (long) timeSource.getTimeSeconds();

        if (lastSecTimestamp == 0) {
            lastSecTimestamp = nowSec;
            startTime = timeSource.getTimeSeconds();
            return;
        }

        long diff = nowSec - lastSecTimestamp;
        if (diff == 0) return;

        // Reset if gap exceeds window
        if (diff >= windowSeconds) {
            clearBuckets();
            lastSecTimestamp = nowSec;
            return;
        }

        // Rolling window update
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

        double nowSec = timeSource.getTimeSeconds();
        double activeDuration = nowSec - startTime;

        // Prevent division by zero and handle warm-up
        double divisor = Math.min(windowSeconds, Math.max(1.0, activeDuration));

        return runningSum * (3600.0 / divisor);
    }

    public void clear() {
        clearBuckets();
        lastSecTimestamp = 0;
        startTime = 0;
        timeSource.reset();
    }

    private void clearBuckets() {
        Arrays.fill(buckets, 0);
        runningSum = 0;
        currentBucketIndex = 0;
    }
}