package com.kjmaster.yield.tracker;

import com.kjmaster.yield.client.GoalToast;
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
    private final RateCalculator xpCalculator = new RateCalculator(RATE_WINDOW_SECONDS);

    // Store the last known count of items to detect changes
    private final Map<Object, Integer> lastCounts = new HashMap<>();
    private int lastTotalXp = -1;

    // NEW: Optimization Flags
    private boolean isDirty = true;
    private int lastInventoryVersion = -1;

    private boolean isRunning = false;
    private long sessionStartTime = 0;

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
        xpCalculator.clear();
        lastCounts.clear();
        lastTotalXp = -1;

        // Force immediate scan on start
        isDirty = true;
        if (Minecraft.getInstance().player != null) {
            lastInventoryVersion = Minecraft.getInstance().player.getInventory().getTimesChanged();
        }
    }

    public void stopSession() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    // NEW: Allow external events to trigger a scan
    public void setDirty() {
        this.isDirty = true;
    }

    public long getSessionDuration() {
        if (!isRunning) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    public void addXpGain(int amount) {
        if (!isRunning || amount <= 0) return;
        var projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isPresent() && projectOpt.get().shouldTrackXp()) {
            xpCalculator.addGain(amount);
        }
    }

    public double getXpPerHour() {
        return xpCalculator.getItemsPerHour();
    }

    /**
     * Called every client tick by the Event Handler
     */
    public void onTick(Player player) {
        if (!isRunning || player == null) return;

        var projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isEmpty()) return;
        YieldProject project = projectOpt.get();

        // 1. Heavy Logic: Inventory Scanning
        // Only run this if the inventory actually changed (Event-Driven)
        if (player.getInventory().getTimesChanged() != lastInventoryVersion) {
            isDirty = true;
            lastInventoryVersion = player.getInventory().getTimesChanged();
        }

        if (isDirty) {
            updateCounts(player, project); // Only counts items
            isDirty = false;
        }

        // 2. Light Logic: Rate Decay & UI Cache
        // Run this EVERY TICK (or every 20 ticks) to ensure the "Items/Hour"
        // drops to zero if the player stops working.
        updateRates(project);

        // 3. XP Logic (Hybrid)
        if (project.shouldTrackXp()) {
            updateXpTracking(player);
        }
    }



    private void updateXpTracking(Player player) {
        int currentXp = player.totalExperience;
        if (lastTotalXp != -1) {
            int diff = currentXp - lastTotalXp;
            if (diff > 0) {
                addXpGain(diff);
            }
        }
        lastTotalXp = currentXp;
    }

    /**
     * Heavy scan: updates actual item counts and feeds data TO the calculators.
     */
    private void updateCounts(Player player, YieldProject project) {
        for (ProjectGoal goal : project.getGoals()) {
            int lastCount = lastCounts.getOrDefault(goal.getItem(), -1);
            int currentCount = countItemInInventory(player, goal);

            // Detect Completion Toast
            if (lastCount != -1 && lastCount < goal.getTargetAmount() && currentCount >= goal.getTargetAmount()) {
                Minecraft.getInstance().getToasts().addToast(new GoalToast(goal));
            }

            // Calculate Delta
            int delta = (lastCount == -1) ? 0 : currentCount - lastCount;
            if (delta > 0) {
                // Feed the calculator
                RateCalculator calc = calculators.computeIfAbsent(goal.getItem(), k -> new RateCalculator(RATE_WINDOW_SECONDS));
                calc.addGain(delta);
            }

            lastCounts.put(goal.getItem(), currentCount);
        }
    }

    /**
     * Light update: pulls data FROM the calculators to update the UI cache.
     * This handles the "Decay" when idle.
     */
    private void updateRates(YieldProject project) {
        for (ProjectGoal goal : project.getGoals()) {
            RateCalculator calc = calculators.get(goal.getItem());
            double rate = (calc == null) ? 0.0 : calc.getItemsPerHour();

            // We read the current count from our local cache map to avoid scanning the inventory again
            int currentCount = lastCounts.getOrDefault(goal.getItem(), 0);

            // Update the goal's transient data for the HUD
            goal.updateCache(currentCount, rate);
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