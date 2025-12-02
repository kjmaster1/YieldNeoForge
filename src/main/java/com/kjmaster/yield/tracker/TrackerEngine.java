package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.service.InventoryScanner;
import com.kjmaster.yield.time.GameTickSource;
import com.kjmaster.yield.time.TimeSource;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackerEngine {

    private final TrackerState state;
    private final InventoryMonitor monitor;
    private final InventoryScanner scanner;
    private final TimeSource timeSource;

    private RateCalculator xpCalculator;
    private int tickCounter = 0;
    private double cachedXpRate = 0.0;

    // Concurrency Controls
    // Use Virtual Threads if available (Java 21), otherwise fallback implicitly handled by Executors.newVirtualThreadPerTaskExecutor()
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

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
        this.isProcessing.set(false);
        this.timeSource.reset();
    }

    public double getXpRate() { return cachedXpRate; }

    public void addXp(int amount) {
        if (xpCalculator != null) xpCalculator.addGain(amount);
    }

    public void onTick(Player player, YieldProject project) {
        if (xpCalculator == null) return;

        // 1. Sync Trackers (Main Thread - Lightweight)
        syncTrackers(project);

        // 2. XP Logic (Main Thread - Lightweight)
        if (project.trackXp()) {
            updateXpTracking(player);
        }

        // 3. Inventory Logic (Async Dispatch)
        // Check native changes to set dirty flag
        monitor.checkForNativeChanges(player);

        if (monitor.isDirty() && !isProcessing.get()) {
            dispatchScan(player, project);
        }

        // 4. Rate Updates (Every 20 ticks)
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            updateRates();
        }
    }

    private void dispatchScan(Player player, YieldProject project) {
        isProcessing.set(true);

        // A. Capture Snapshot (Main Thread)
        // This copies ItemStacks, ensuring thread safety
        List<ItemStack> snapshot = scanner.createSnapshot(player);

        // Reset dirty flag immediately after snapshot
        monitor.clearDirty();

        // B. Process Logic (Async Thread)
        CompletableFuture.supplyAsync(() -> performMatching(snapshot, project), asyncExecutor)
                .thenAcceptAsync(results -> {
                    // C. Apply Results (Main Thread)
                    applyResults(results);
                    isProcessing.set(false);
                }, Minecraft.getInstance()); // Execute callback on Render Thread
    }

    /**
     * Runs on Virtual Thread. matches snapshot against goals.
     */
    private Map<UUID, Integer> performMatching(List<ItemStack> snapshot, YieldProject project) {
        Map<UUID, Integer> counts = new HashMap<>();

        // Initialize counts to 0
        for (ProjectGoal goal : project.goals()) {
            counts.put(goal.id(), 0);
        }

        // Scan snapshot
        for (ItemStack stack : snapshot) {
            if (stack.isEmpty()) continue;

            for (ProjectGoal goal : project.goals()) {
                if (ItemMatcher.matches(stack, goal)) {
                    counts.merge(goal.id(), stack.getCount(), Integer::sum);
                }
            }
        }
        return counts;
    }

    private void applyResults(Map<UUID, Integer> results) {
        for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
            GoalTracker tracker = state.getTrackers().get(entry.getKey());
            if (tracker != null) {
                tracker.update(entry.getValue());
            }
        }
    }

    private void syncTrackers(YieldProject project) {
        // We still need to create trackers for new goals on the main thread
        // to ensure the UI can query them immediately, even if counts are 0.
        for (ProjectGoal goal : project.goals()) {
            GoalTracker tracker = state.getTrackers().get(goal.id());
            if (tracker == null) {
                state.getTrackers().put(goal.id(), new GoalTracker(goal, this.timeSource));
            } else {
                tracker.updateGoalDefinition(goal);
            }
        }

        // Remove deleted
        Set<UUID> activeIds = new java.util.HashSet<>();
        project.goals().forEach(g -> activeIds.add(g.id()));
        state.getTrackers().keySet().removeIf(id -> !activeIds.contains(id));
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