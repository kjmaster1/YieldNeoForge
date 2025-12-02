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
        // If we are only looking for a specific dirty item, skip unrelated stacks immediately.
        if (targetItemFilter != null && stack.getItem() != targetItemFilter) return;

        // Fast Lookup: O(1) retrieval of relevant trackers
        List<GoalTracker> specific = itemTrackers.get(stack.getItem());

        if (specific != null) {
            // Optimization: Use indexed loop to avoid Iterator allocation in hot path (every slot, every tick)
            for (int i = 0; i < specific.size(); i++) {
                GoalTracker tracker = specific.get(i);

                // ItemMatcher checks Strict Mode (Components) and Tag membership.
                // Identity check for Item ID is implicitly done by the Map lookup,
                // but ItemMatcher validates it again for safety and Tag logic.
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }
    }
}