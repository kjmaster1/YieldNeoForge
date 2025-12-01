package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosInventoryProvider;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryScanner {

    private final List<IInventoryProvider> providers = new ArrayList<>();

    public InventoryScanner() {
        // Always add Vanilla support
        providers.add(new VanillaInventoryProvider());

        // Conditionally add Curios support
        if (ModList.get().isLoaded("curios")) {
            providers.add(new CuriosInventoryProvider());
        }
    }

    public void updateTrackerCounts(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers) {
        scan(player, itemTrackers, tagTrackers, null);
    }

    public void updateSpecificCounts(Player player, Item targetItem, List<GoalTracker> trackers) {
        Map<Item, List<GoalTracker>> specificMap = Map.of(targetItem, trackers);
        scan(player, specificMap, null, targetItem);
    }

    private void scan(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers, Item targetItemFilter) {
        for (IInventoryProvider provider : providers) {
            provider.scan(player, itemTrackers, tagTrackers, targetItemFilter);
        }
    }
}