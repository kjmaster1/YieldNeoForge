package com.kjmaster.yield.api;

import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.tracker.GoalTracker;

public interface ISessionStatus {
    /**
     * Checks if a session is currently running.
     *
     * @return true if running, false otherwise.
     */
    boolean isRunning();

    /**
     * Gets the duration of the current session in milliseconds.
     *
     * @return Duration in ms.
     */
    long getSessionDuration();

    /**
     * Retrieves or creates a tracker for a specific goal.
     *
     * @param goal The project goal to track.
     * @return The GoalTracker instance.
     */
    GoalTracker getTracker(ProjectGoal goal);

    /**
     * Gets the current calculated XP per hour.
     *
     * @return XP per hour.
     */
    double getXpPerHour();
}