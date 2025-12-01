package com.kjmaster.yield.service;

import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

public interface IInventoryProvider {
    /**
     * Scans the inventory source and updates the trackers using ScannerHelper.
     *
     * @param player           The player entity.
     * @param itemTrackers     Map of item-specific trackers.
     * @param tagTrackers      List of tag-based trackers.
     * @param targetItemFilter Optional item to filter the scan (optimization).
     */
    void scan(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers, Item targetItemFilter);
}