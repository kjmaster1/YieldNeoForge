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

    private boolean isCompleted = false;
    private boolean hasToastFired = false;

    public GoalTracker(ProjectGoal goal, TimeSource timeSource) {
        this.goal = goal;
        this.calculator = new RateCalculator(Config.RATE_WINDOW.get(), timeSource);
    }

    public void updateGoalDefinition(ProjectGoal newGoal) {
        this.goal = newGoal;
        checkCompletionState(this.currentCount);
    }

    public void update(int newCount) {
        if (startCount == -1) {
            startCount = newCount;
            currentCount = newCount;
            checkCompletionState(currentCount);
            // Don't toast on initial load, just mark fired if already complete
            if (isCompleted) {
                hasToastFired = true;
            }
            return;
        }

        int delta = newCount - currentCount;
        if (delta > 0) {
            calculator.addGain(delta);
        }

        checkCompletionState(newCount);
        this.currentCount = newCount;
    }

    private void checkCompletionState(int count) {
        // 1. Status Logic: Always reflect reality
        this.isCompleted = count >= goal.targetAmount();

        // 2. Notification Logic: Hysteresis
        if (this.isCompleted) {
            if (!hasToastFired) {
                hasToastFired = true;
                Minecraft.getInstance().getToasts().addToast(new GoalToast(goal));
            }
        } else {
            // Only re-arm the toast if we drop significantly below the target (Hysteresis)
            // Example: If target is 64, we must drop to 62 or lower to re-arm.
            // This prevents spam if the user oscillates between 63 and 64.
            if (count < goal.targetAmount() - 1) {
                hasToastFired = false;
            }
        }
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