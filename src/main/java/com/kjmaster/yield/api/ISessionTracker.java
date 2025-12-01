package com.kjmaster.yield.api;

import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.world.entity.player.Player;

public interface ISessionTracker {

    /**
     * Starts a new tracking session. Resets counters and rates.
     */
    void startSession();

    /**
     * Stops the current tracking session.
     */
    void stopSession();

    /**
     * Checks if a session is currently running.
     *
     * @return true if running, false otherwise.
     */
    boolean isRunning();

    /**
     * Marks the inventory state as dirty, forcing a re-scan on the next tick.
     */
    void setDirty();

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
     * Adds an XP gain amount to the XP rate calculator.
     *
     * @param amount The amount of XP gained.
     */
    void addXpGain(int amount);

    /**
     * Gets the current calculated XP per hour.
     *
     * @return XP per hour.
     */
    double getXpPerHour();

    /**
     * Tick handler to update tracking logic.
     *
     * @param player The client player entity.
     */
    void onTick(Player player);
}