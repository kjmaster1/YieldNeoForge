package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.service.InventoryScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionTracker {

    private static SessionTracker INSTANCE;

    private final Map<ProjectGoal, GoalTracker> trackers = new ConcurrentHashMap<>();

    // Optimization Caches
    private final Map<Item, List<GoalTracker>> itemSpecificTrackers = new HashMap<>();
    private final List<GoalTracker> tagTrackers = new ArrayList<>();

    private RateCalculator xpCalculator = new RateCalculator(15);
    private final InventoryScanner scanner = new InventoryScanner();

    private int lastTotalXp = -1;
    private boolean isDirty = true;
    private int lastInventoryVersion = -1;
    private boolean isRunning = false;
    private long sessionStartTime = 0;

    // Phase 3: Ticking State
    private int tickCounter = 0;
    private double cachedXpRate = 0.0;

    private SessionTracker() {
    }

    public static synchronized SessionTracker get() {
        if (INSTANCE == null) {
            INSTANCE = new SessionTracker();
        }
        return INSTANCE;
    }

    public void startSession() {
        this.xpCalculator = new RateCalculator(Config.RATE_WINDOW.get());
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();

        trackers.clear();
        itemSpecificTrackers.clear();
        tagTrackers.clear();

        xpCalculator.clear();
        lastTotalXp = -1;
        isDirty = true;
        tickCounter = 0;
        cachedXpRate = 0.0;

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

    public void setDirty() {
        this.isDirty = true;
    }

    public long getSessionDuration() {
        if (!isRunning) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    public GoalTracker getTracker(ProjectGoal goal) {
        return trackers.computeIfAbsent(goal, GoalTracker::new);
    }

    public void addXpGain(int amount) {
        if (!isRunning || amount <= 0) return;
        var projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isPresent() && projectOpt.get().shouldTrackXp()) {
            xpCalculator.addGain(amount);
        }
    }

    public double getXpPerHour() {
        return cachedXpRate; // Return cached value
    }

    public void onTick(Player player) {
        if (!isRunning || player == null) return;

        var projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isEmpty()) return;
        YieldProject project = projectOpt.get();

        // 1. Heavy Logic: Inventory Scanning (Event Driven)
        if (player.getInventory().getTimesChanged() != lastInventoryVersion) {
            isDirty = true;
            lastInventoryVersion = player.getInventory().getTimesChanged();
        }

        if (isDirty) {
            updateTrackers(player, project);
            isDirty = false;
        }

        // 2. XP Logic (Hybrid)
        if (project.shouldTrackXp()) {
            updateXpTracking(player);
        }

        // 3. Periodic Logic: Rate Calculation (Every 1 Second / 20 Ticks)
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            updateRates();
        }
    }

    private void updateRates() {
        // Update all Goal Rates
        for (GoalTracker tracker : trackers.values()) {
            tracker.updateRate();
        }
        // Update XP Rate
        this.cachedXpRate = xpCalculator.getItemsPerHour();
    }

    private void updateTrackers(Player player, YieldProject project) {
        // Ensure trackers exist for all active goals
        boolean changed = false;
        for (ProjectGoal goal : project.getGoals()) {
            if (!trackers.containsKey(goal)) {
                trackers.put(goal, new GoalTracker(goal));
                changed = true;
            }
        }

        // Rebuild Lookup Cache if needed (or if empty)
        // Since this only happens when inventory is dirty (rare), rebuilding is cheap.
        if (changed || (itemSpecificTrackers.isEmpty() && tagTrackers.isEmpty() && !trackers.isEmpty())) {
            rebuildLookupCache();
        }

        // 1. Reset Temp Counts
        for (GoalTracker tracker : trackers.values()) {
            tracker.resetTempCount();
        }

        // 2. Scan Inventory (Using Optimized Cache)
        scanner.updateTrackerCounts(player, itemSpecificTrackers, tagTrackers);

        // 3. Commit Counts (Updates logic/rates/toasts)
        for (GoalTracker tracker : trackers.values()) {
            tracker.commitCounts();
        }
    }

    private void rebuildLookupCache() {
        itemSpecificTrackers.clear();
        tagTrackers.clear();

        for (GoalTracker tracker : trackers.values()) {
            ProjectGoal goal = tracker.getGoal();

            if (goal.getTargetTag().isPresent()) {
                // Tag goals must be checked against everything (or filtered by tag, but iterating list is safer)
                tagTrackers.add(tracker);
            } else {
                // Item goals can be hashed by Item Type
                itemSpecificTrackers
                        .computeIfAbsent(goal.getItem(), k -> new ArrayList<>())
                        .add(tracker);
            }
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
}