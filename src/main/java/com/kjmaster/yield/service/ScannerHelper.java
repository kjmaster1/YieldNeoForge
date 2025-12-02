package com.kjmaster.yield.service;

import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class ScannerHelper {

    /**
     * Shared logic to check a stack against trackers and increment counts.
     * Uses the optimized Inverted Index from TrackerState.
     */
    public static void checkAndIncrement(ItemStack stack, Map<Item, List<GoalTracker>> itemTrackers, Item targetItemFilter) {
        if (stack.isEmpty()) return;

        // Optimization: Granular Scan Filter
        if (targetItemFilter != null && stack.getItem() != targetItemFilter) return;

        // Fast Lookup: O(1) retrieval of relevant trackers
        List<GoalTracker> specific = itemTrackers.get(stack.getItem());

        if (specific != null) {
            for (GoalTracker tracker : specific) {
                // ItemMatcher is still needed to check "Strict Mode" (NBT/Components)
                // But we already know the Item ID (or Tag membership) matches.
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }
    }
}