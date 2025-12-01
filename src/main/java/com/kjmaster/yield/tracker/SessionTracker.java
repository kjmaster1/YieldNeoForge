package com.kjmaster.yield.tracker;

import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.service.InventoryScanner;
import com.kjmaster.yield.util.ItemMatcher;
import com.kjmaster.yield.util.StackKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionTracker {

    private static SessionTracker INSTANCE;
    private static final int RATE_WINDOW_SECONDS = 60;

    private final Map<ProjectGoal, GoalTracker> trackers = new ConcurrentHashMap<>();
    private final RateCalculator xpCalculator = new RateCalculator(RATE_WINDOW_SECONDS);
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
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();
        trackers.clear();
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
        // PERF: Scan inventory ONCE to build a snapshot
        Map<StackKey, Integer> snapshot = scanner.scanInventory(player);

        for (ProjectGoal goal : project.getGoals()) {
            GoalTracker tracker = trackers.computeIfAbsent(goal, GoalTracker::new);
            int currentCount = 0;

            // Iterate snapshot to find matches
            // This is efficient because snapshot size is small (inventory slots)
            for (Map.Entry<StackKey, Integer> entry : snapshot.entrySet()) {
                if (ItemMatcher.matches(entry.getKey().stack(), goal)) {
                    currentCount += entry.getValue();
                }
            }
            tracker.update(currentCount);
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