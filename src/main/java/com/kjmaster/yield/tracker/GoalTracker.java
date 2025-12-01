package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.client.GoalToast;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.time.TimeSource;
import net.minecraft.client.Minecraft;

public class GoalTracker {
    private final ProjectGoal goal;
    private final RateCalculator calculator;

    // Runtime State
    private int currentCount = 0;
    private int startCount = -1;

    // Cache: Updated once per second, read every frame
    private double cachedRate = 0.0;

    // Scanning Buffer (Prevents object allocation during scans)
    private int tempCount = 0;

    public GoalTracker(ProjectGoal goal, TimeSource timeSource) {
        this.goal = goal;
        this.calculator = new RateCalculator(Config.RATE_WINDOW.get(), timeSource);
    }

    public void update(int newCount) {
        if (startCount == -1) {
            startCount = newCount;
            currentCount = newCount;
            return;
        }

        int delta = newCount - currentCount;
        if (delta > 0) {
            calculator.addGain(delta);
        }

        if (currentCount < goal.getTargetAmount() && newCount >= goal.getTargetAmount()) {
            Minecraft.getInstance().getToasts().addToast(new GoalToast(goal));
        }

        this.currentCount = newCount;
    }

    // --- Scanning Buffer Methods ---

    /**
     * Resets the temporary scan count to zero.
     * Call this before starting a new inventory scan.
     */
    public void resetTempCount() {
        this.tempCount = 0;
    }

    /**
     * Adds to the temporary scan count.
     * Used by the InventoryScanner when a matching item is found.
     */
    public void incrementTempCount(int amount) {
        this.tempCount += amount;
    }

    /**
     * Commits the temporary count to the main logic.
     * Call this after the inventory scan is complete.
     */
    public void commitCounts() {
        update(this.tempCount);
    }

    /**
     * Triggers a recalculation of the rate.
     * Should be called periodically (e.g. every 20 ticks).
     */
    public void updateRate() {
        this.cachedRate = calculator.getItemsPerHour();
    }

    public ProjectGoal getGoal() {
        return goal;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public double getItemsPerHour() {
        return cachedRate;
    }

    public float getProgress() {
        if (goal.getTargetAmount() == 0) return 0f;
        return Math.min(1.0f, (float) currentCount / goal.getTargetAmount());
    }
}