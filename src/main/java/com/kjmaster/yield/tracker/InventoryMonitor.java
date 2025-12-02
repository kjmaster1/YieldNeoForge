package com.kjmaster.yield.tracker;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Tracks inventory state changes to determine when a scan is necessary.
 * Now simplified to act as a coalescing "Tick Gate".
 */
public class InventoryMonitor {

    private boolean dirty = true;
    private int lastInventoryVersion = -1;

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
        // Minecraft's inventory revision counter increments on ANY change.
        int currentVersion = player.getInventory().getTimesChanged();
        if (currentVersion != lastInventoryVersion) {
            this.dirty = true;
            lastInventoryVersion = currentVersion;
        }
    }
}