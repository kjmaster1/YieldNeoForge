package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosScanner;
import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Map;

public class InventoryScanner {

    private final boolean isCuriosLoaded;

    public InventoryScanner() {
        this.isCuriosLoaded = ModList.get().isLoaded("curios");
    }

    /**
     * Scans inventory and updates the temp counts of the provided trackers.
     * This avoids creating a snapshot Map or StackKeys, reducing object churn.
     */
    public void updateTrackerCounts(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers) {
        // 1. Scan Capability Inventory (or Fallback to Vanilla)
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            // Capability support (e.g. modded containers or uniform access)
            for (int i = 0; i < handler.getSlots(); i++) {
                checkAndIncrement(handler.getStackInSlot(i), itemTrackers, tagTrackers);
            }
        } else {
            // Vanilla Fallback
            for (ItemStack stack : player.getInventory().items) checkAndIncrement(stack, itemTrackers, tagTrackers);
            for (ItemStack stack : player.getInventory().armor) checkAndIncrement(stack, itemTrackers, tagTrackers);
            for (ItemStack stack : player.getInventory().offhand) checkAndIncrement(stack, itemTrackers, tagTrackers);
        }

        // 2. Scan Curios
        if (isCuriosLoaded) {
            CuriosScanner.scanCurios(player, itemTrackers, tagTrackers);
        }
    }

    private void checkAndIncrement(ItemStack stack, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers) {
        if (stack.isEmpty()) return;

        // 1. Fast Lookup: Check Item-Specific Trackers
        // This reduces complexity to O(1) for finding relevant goals
        List<GoalTracker> specific = itemTrackers.get(stack.getItem());
        if (specific != null) {
            for (GoalTracker tracker : specific) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }

        // 2. Tag Lookup: Must check all Tag-based goals
        // Usually there are very few of these compared to item goals.
        if (tagTrackers != null && !tagTrackers.isEmpty()) {
            for (GoalTracker tracker : tagTrackers) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }
    }
}