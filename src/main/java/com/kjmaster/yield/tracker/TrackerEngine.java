package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.service.InventoryScanner;
import com.kjmaster.yield.time.GameTickSource;
import com.kjmaster.yield.time.TimeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TrackerEngine {

    private final TrackerState state;
    private final InventoryMonitor monitor;
    private final InventoryScanner scanner;
    private final TimeSource timeSource;

    private RateCalculator xpCalculator;
    private int tickCounter = 0;
    private double cachedXpRate = 0.0;

    // Performance Throttling
    private int scanThrottleCounter = 0;
    private static final int SCAN_INTERVAL = 4;

    public TrackerEngine(TrackerState state, InventoryMonitor monitor) {
        this.state = state;
        this.monitor = monitor;
        this.scanner = new InventoryScanner();
        this.timeSource = new GameTickSource();
    }

    public TimeSource getTimeSource() { return timeSource; }

    public void reset() {
        this.xpCalculator = new RateCalculator(Config.RATE_WINDOW.get(), timeSource);
        this.xpCalculator.clear();
        this.tickCounter = 0;
        this.cachedXpRate = 0.0;
        this.scanThrottleCounter = 0;
        this.timeSource.reset();
    }

    public double getXpRate() { return cachedXpRate; }

    public void addXp(int amount) {
        if (xpCalculator != null) xpCalculator.addGain(amount);
    }

    public void onTick(Player player, YieldProject project) {
        if (xpCalculator == null) return;

        // 1. Sync Trackers (Handle Additions, Removals, and Modifications)
        syncTrackers(project);

        // 2. XP Logic
        if (project.trackXp()) {
            updateXpTracking(player);
        }

        // 3. Inventory Logic
        scanThrottleCounter++;
        if (scanThrottleCounter >= SCAN_INTERVAL) {
            scanThrottleCounter = 0;
            processInventoryUpdates(player);
        }

        // 4. Rate Updates
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            updateRates();
        }
    }

    private void syncTrackers(YieldProject project) {
        boolean cacheRebuildNeeded = false;
        Set<UUID> activeGoalIds = new HashSet<>();

        // A. Update or Add Goals
        for (ProjectGoal goal : project.goals()) {
            activeGoalIds.add(goal.id());
            GoalTracker tracker = state.getTrackers().get(goal.id());

            if (tracker == null) {
                // New Goal -> Create Tracker
                state.getTrackers().put(goal.id(), new GoalTracker(goal, this.timeSource));
                cacheRebuildNeeded = true;
            } else {
                // Existing Goal -> Check for structural changes
                ProjectGoal oldGoal = tracker.getGoal();

                // Always update the definition to capture Amount changes
                tracker.updateGoalDefinition(goal);

                // If Item, Tag, or Strictness changed, we MUST rebuild the lookup map
                if (isStructuralChange(oldGoal, goal)) {
                    cacheRebuildNeeded = true;
                }
            }
        }

        // B. Remove Deleted Goals
        // If a tracker exists in state but its ID is not in the current project, remove it.
        boolean removed = state.getTrackers().keySet().removeIf(uuid -> !activeGoalIds.contains(uuid));
        if (removed) {
            cacheRebuildNeeded = true;
        }

        // C. Rebuild Cache if necessary
        if (cacheRebuildNeeded || (state.getItemSpecificTrackers().isEmpty() && !state.getTrackers().isEmpty())) {
            rebuildLookupCache();
        }
    }

    private boolean isStructuralChange(ProjectGoal g1, ProjectGoal g2) {
        // Returns true if any property affecting the Scanner Map has changed
        return !g1.item().equals(g2.item()) ||
                !g1.targetTag().equals(g2.targetTag()) ||
                g1.strict() != g2.strict();
    }

    private void processInventoryUpdates(Player player) {
        monitor.checkForNativeChanges(player);
        if (monitor.isAllDirty()) {
            performFullScan(player);
            monitor.clearAllDirty();
        } else if (!monitor.getDirtyItems().isEmpty()) {
            performGranularScan(player);
            monitor.clearAllDirty();
        }
    }

    private void rebuildLookupCache() {
        state.getItemSpecificTrackers().clear();
        for (GoalTracker tracker : state.getTrackers().values()) {
            ProjectGoal goal = tracker.getGoal();
            if (goal.targetTag().isPresent()) {
                var tagKey = goal.targetTag().get();
                for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                    state.getItemSpecificTrackers()
                            .computeIfAbsent(holder.value(), k -> new ArrayList<>())
                            .add(tracker);
                }
            } else {
                state.getItemSpecificTrackers()
                        .computeIfAbsent(goal.item(), k -> new ArrayList<>())
                        .add(tracker);
            }
        }
    }

    private void performFullScan(Player player) {
        for (GoalTracker tracker : state.getTrackers().values()) tracker.resetTempCount();
        scanner.updateTrackerCounts(player, state.getItemSpecificTrackers());
        for (GoalTracker tracker : state.getTrackers().values()) tracker.commitCounts();
    }

    private void performGranularScan(Player player) {
        for (Item item : monitor.getDirtyItems()) {
            List<GoalTracker> trackers = state.getItemSpecificTrackers().get(item);
            if (trackers != null) {
                for (GoalTracker t : trackers) t.resetTempCount();
                scanner.updateSpecificCounts(player, item, trackers);
                for (GoalTracker t : trackers) t.commitCounts();
            }
        }
    }

    private void updateXpTracking(Player player) {
        int currentXp = player.totalExperience;
        if (state.getLastTotalXp() != -1) {
            int diff = currentXp - state.getLastTotalXp();
            if (diff > 0) xpCalculator.addGain(diff);
        }
        state.setLastTotalXp(currentXp);
    }

    private void updateRates() {
        for (GoalTracker tracker : state.getTrackers().values()) {
            tracker.updateRate();
        }
        if (xpCalculator != null) {
            this.cachedXpRate = xpCalculator.getItemsPerHour();
        }
    }
}