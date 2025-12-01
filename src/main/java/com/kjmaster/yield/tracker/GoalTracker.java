package com.kjmaster.yield.tracker;

import com.kjmaster.yield.client.GoalToast;
import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.client.Minecraft;

public class GoalTracker {
    private final ProjectGoal goal;
    private final RateCalculator calculator;

    // Runtime State
    private int currentCount = 0;
    private int startCount = -1;

    // Cache: Updated once per second, read every frame
    private double cachedRate = 0.0;

    public GoalTracker(ProjectGoal goal) {
        this.goal = goal;
        this.calculator = new RateCalculator(60);
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