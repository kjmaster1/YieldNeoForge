package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosScanner;
import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collection;

public class InventoryScanner {

    private final boolean isCuriosLoaded;

    public InventoryScanner() {
        this.isCuriosLoaded = ModList.get().isLoaded("curios");
    }

    /**
     * Scans inventory and updates the temp counts of the provided trackers.
     * This avoids creating a snapshot Map or StackKeys, reducing object churn.
     */
    public void updateTrackerCounts(Player player, Collection<GoalTracker> activeTrackers) {
        // 1. Scan Capability Inventory (or Fallback to Vanilla)
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            // Capability support (e.g. modded containers or uniform access)
            for (int i = 0; i < handler.getSlots(); i++) {
                checkAndIncrement(handler.getStackInSlot(i), activeTrackers);
            }
        } else {
            // Vanilla Fallback
            for (ItemStack stack : player.getInventory().items) checkAndIncrement(stack, activeTrackers);
            for (ItemStack stack : player.getInventory().armor) checkAndIncrement(stack, activeTrackers);
            for (ItemStack stack : player.getInventory().offhand) checkAndIncrement(stack, activeTrackers);
        }

        // 2. Scan Curios
        if (isCuriosLoaded) {
            CuriosScanner.scanCurios(player, activeTrackers);
        }
    }

    private void checkAndIncrement(ItemStack stack, Collection<GoalTracker> activeTrackers) {
        if (stack.isEmpty()) return;

        // Iterate active trackers to find a match.
        // This is O(InventorySlots * NumGoals). Since NumGoals is small, this is highly efficient.
        for (GoalTracker tracker : activeTrackers) {
            if (ItemMatcher.matches(stack, tracker.getGoal())) {
                tracker.incrementTempCount(stack.getCount());
            }
        }
    }
}