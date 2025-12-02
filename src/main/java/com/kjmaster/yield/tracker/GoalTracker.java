package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.client.GoalToast;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.time.TimeSource;
import net.minecraft.client.Minecraft;

public class GoalTracker {
    private ProjectGoal goal;
    private final RateCalculator calculator;

    private int currentCount = 0;
    private int startCount = -1;
    private double cachedRate = 0.0;
    private int tempCount = 0;

    // Fix: Persistent completion flag to prevent oscillation spam
    private boolean isCompleted = false;

    public GoalTracker(ProjectGoal goal, TimeSource timeSource) {
        this.goal = goal;
        this.calculator = new RateCalculator(Config.RATE_WINDOW.get(), timeSource);
    }

    // Allow updating the goal definition (e.g. target amount changed)
    public void updateGoalDefinition(ProjectGoal newGoal) {
        this.goal = newGoal;
        // Reset completion status if the new target is higher than what we have
        if (this.currentCount < newGoal.targetAmount()) {
            this.isCompleted = false;
        }
    }

    public void update(int newCount) {
        if (startCount == -1) {
            startCount = newCount;
            currentCount = newCount;
            // Initialize state: if loaded with enough items, mark complete silently (no toast)
            if (currentCount >= goal.targetAmount()) {
                this.isCompleted = true;
            }
            return;
        }

        int delta = newCount - currentCount;
        if (delta > 0) {
            calculator.addGain(delta);
        }

        // Fix: Use latched state to determine if we should toast
        if (!isCompleted && newCount >= goal.targetAmount()) {
            this.isCompleted = true;
            Minecraft.getInstance().getToasts().addToast(new GoalToast(goal));
        }

        this.currentCount = newCount;
    }

    public void resetTempCount() {
        this.tempCount = 0;
    }

    public void incrementTempCount(int amount) {
        this.tempCount += amount;
    }

    public void commitCounts() {
        update(this.tempCount);
    }

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
        if (goal.targetAmount() == 0) return 0f;
        return Math.min(1.0f, (float) currentCount / goal.targetAmount());
    }
}