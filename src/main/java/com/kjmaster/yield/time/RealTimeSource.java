package com.kjmaster.yield.time;

public class RealTimeSource implements TimeSource {
    @Override
    public double getTimeSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    @Override
    public void reset() {
        // No state to reset
    }
}