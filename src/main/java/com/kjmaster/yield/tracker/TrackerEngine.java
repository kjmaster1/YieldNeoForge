package com.kjmaster.yield.tracker;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.service.InventoryScanner;
import com.kjmaster.yield.time.GameTickSource;
import com.kjmaster.yield.time.TimeSource;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
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

    private final Map<EquipmentSlot, ItemStack> lastEquipment = new EnumMap<>(EquipmentSlot.class);
    private int lastSelectedSlot = -1;

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

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public void reset() {
        this.xpCalculator = new RateCalculator(Config.RATE_WINDOW.get(), timeSource);
        this.xpCalculator.clear();
        this.tickCounter = 0;
        this.cachedXpRate = 0.0;
        this.isProcessing.set(false);
        this.timeSource.reset();
    }

    public double getXpRate() {
        return cachedXpRate;
    }

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
        // 1. Standard XP Bar Tracking
        int currentXp = player.totalExperience;
        if (state.getLastTotalXp() != -1) {
            int diff = currentXp - state.getLastTotalXp();
            if (diff > 0) xpCalculator.addGain(diff);
        }
        state.setLastTotalXp(currentXp);

        // 2. Mending Tracking (Enhanced for GUI support)
        // We NO LONGER check "screen == null".
        // Instead, we rely on strict component matching to filter out swaps.

        int currentSlot = player.getInventory().selected;
        boolean slotChanged = currentSlot != lastSelectedSlot;
        lastSelectedSlot = currentSlot;

        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack currentStack = player.getItemBySlot(slot);
            ItemStack lastStack = lastEquipment.get(slot);

            // Update cache (Deep copy is crucial)
            lastEquipment.put(slot, currentStack.copy());

            if (lastStack == null || lastStack.isEmpty() || currentStack.isEmpty()) continue;

            // Skip MainHand if the hotbar selection just changed
            if (slot == EquipmentSlot.MAINHAND && slotChanged) continue;

            // STRICT IDENTITY CHECK
            // 1. Must be same Item Type (e.g. Diamond Pickaxe)
            if (!ItemStack.isSameItem(lastStack, currentStack)) continue;

            // 2. Must have identical components (Enchants, Name, Lore, etc.)
            // We only allow DAMAGE to differ.
            if (!ItemMatcher.checkContains(lastStack, currentStack, Set.of(DataComponents.DAMAGE))) continue;

            // 3. Calculate Repair
            int damageDiff = lastStack.getDamageValue() - currentStack.getDamageValue();

            final int MAX_REPAIR_PER_TICK = 100;

            if (damageDiff > 0 && damageDiff <= MAX_REPAIR_PER_TICK) {
                // 1 XP = 2 Durability. Round up to catch 1-point repairs.
                int xpConsumed = (int) Math.ceil(damageDiff / 2.0);
                xpCalculator.addGain(xpConsumed);
            }
        }
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