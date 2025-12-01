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
     */
    public static void checkAndIncrement(ItemStack stack, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers, Item targetItemFilter) {
        if (stack.isEmpty()) return;

        // Optimization: Filter by target item if strictly scanning for one type (Granular Scan)
        if (targetItemFilter != null && stack.getItem() != targetItemFilter) return;

        // 1. Fast Lookup: Check Item-Specific Trackers
        List<GoalTracker> specific = itemTrackers.get(stack.getItem());
        if (specific != null) {
            for (GoalTracker tracker : specific) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }

        // 2. Tag Lookup: Check Tag-based goals
        if (tagTrackers != null && !tagTrackers.isEmpty()) {
            for (GoalTracker tracker : tagTrackers) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }
    }
}