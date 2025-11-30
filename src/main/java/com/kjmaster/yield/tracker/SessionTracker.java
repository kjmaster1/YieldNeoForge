package com.kjmaster.yield.tracker;

import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SessionTracker {

    private static SessionTracker INSTANCE;

    // Config: How many seconds to look back for the rate (smoothing)
    private static final int RATE_WINDOW_SECONDS = 60;

    // Store a RateCalculator for each Item Type (using Item object as key)
    private final Map<Object, RateCalculator> calculators = new HashMap<>();

    // Store the last known count of items to detect changes
    private final Map<Object, Integer> lastCounts = new HashMap<>();

    private boolean isRunning = false;
    private long sessionStartTime = 0;
    private int tickCounter = 0;

    private SessionTracker() {}

    public static synchronized SessionTracker get() {
        if (INSTANCE == null) {
            INSTANCE = new SessionTracker();
        }
        return INSTANCE;
    }

    public void startSession() {
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();
        calculators.clear();
        lastCounts.clear();
    }

    public void pauseSession() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public long getSessionDuration() {
        if (!isRunning) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Called every client tick by the Event Handler
     */
    public void onTick(Player player) {
        if (!isRunning || player == null) return;

        // Optimization: Only scan inventory every 10 ticks (0.5 seconds)
        // This is fast enough for UI updates but saves CPU cycles.
        tickCounter++;
        if (tickCounter < 10) return;
        tickCounter = 0;

        updateTracking(player);
    }

    private void updateTracking(Player player) {
        var projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isEmpty()) return;
        YieldProject project = projectOpt.get();

        for (ProjectGoal goal : project.getGoals()) {
            // 1. Get previous state
            int lastCount = lastCounts.getOrDefault(goal.getItem(), -1); // -1 indicates first run/init

            // 2. Get current state
            int currentCount = countItemInInventory(player, goal);

            // 3. Detect "Crossing the Finish Line"
            // We only trigger if we weren't done before (lastCount < target),
            // and we are done now (currentCount >= target).
            // We also ignore the very first tick (lastCount == -1) to prevent login spam.
            if (lastCount != -1 && lastCount < goal.getTargetAmount() && currentCount >= goal.getTargetAmount()) {
                // FIRE TOAST!
                Minecraft.getInstance().getToasts().addToast(new com.kjmaster.yield.client.GoalToast(goal));
            }

            // ... Existing Rate Calculation Logic ...
            int delta = (lastCount == -1) ? 0 : currentCount - lastCount;

            RateCalculator calc = calculators.computeIfAbsent(goal.getItem(), k -> new RateCalculator(RATE_WINDOW_SECONDS));
            if (delta > 0) calc.addGain(delta);

            goal.updateCache(currentCount, calc.getItemsPerHour());
            lastCounts.put(goal.getItem(), currentCount);
        }
    }

    private int countItemInInventory(Player player, ProjectGoal goal) {
        int count = 0;
        // Search main inventory
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == goal.getItem()) {
                count += stack.getCount();
            }
        }
        // Search offhand
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() == goal.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
