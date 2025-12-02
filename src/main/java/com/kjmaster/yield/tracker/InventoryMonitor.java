package com.kjmaster.yield.tracker;

import com.kjmaster.yield.compat.curios.CuriosInventoryWatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks inventory state changes to determine when a scan is necessary.
 * Now simplified to act as a coalescing "Tick Gate".
 */
public class InventoryMonitor {

    private boolean dirty = true;
    private final List<Strategy> strategies = new ArrayList<>();

    public InventoryMonitor() {
        // 1. Vanilla Strategy
        strategies.add(new VanillaStrategy());

        // 2. Curios Strategy (Conditional)
        if (ModList.get().isLoaded("curios")) {
            strategies.add(new CuriosInventoryWatcher());
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    // Legacy support for event handlers (mapped to general dirty)
    public void markItemDirty(Item item) {
        markDirty();
    }
    public void markAllDirty() {
        markDirty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public void checkForNativeChanges(Player player) {
        // Poll all registered strategies
        for (Strategy strategy : strategies) {
            if (strategy.isDirty(player)) {
                this.dirty = true;
                // We can break early if dirty, but some strategies might need to update their internal state
                // regardless (like syncing a version number). Ideally, 'isDirty' updates state and returns result.
            }
        }
    }

    public interface Strategy {
        boolean isDirty(Player player);
    }

    private static class VanillaStrategy implements Strategy {
        private int lastInventoryVersion = -1;

        @Override
        public boolean isDirty(Player player) {
            // Minecraft's inventory revision counter increments on ANY change.
            int currentVersion = player.getInventory().getTimesChanged();
            if (currentVersion != lastInventoryVersion) {
                lastInventoryVersion = currentVersion;
                return true;
            }
            return false;
        }
    }
}