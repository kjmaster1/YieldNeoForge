package com.kjmaster.yield.time;

public interface TimeSource {
    /**
     * Returns the current time in seconds.
     * Use double to support partial seconds (e.g. ticks).
     */
    double getTimeSeconds();

    /**
     * Resets the internal state if necessary (e.g. start time).
     */
    void reset();
}