package com.kjmaster.yield.tracker;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

public class InventoryMonitor {

    private boolean allDirty = true;
    private final Set<Item> dirtyItems = new HashSet<>();
    private int lastInventoryVersion = -1;

    // Renamed to markAllDirty for clarity and consistency with the refactor plan
    public void markAllDirty() {
        this.allDirty = true;
    }

    // New method to mark a single item as dirty
    public void markItemDirty(Item item) {
        this.dirtyItems.add(item);
    }

    public boolean isAllDirty() {
        return allDirty;
    }

    public Set<Item> getDirtyItems() {
        return dirtyItems;
    }

    // Renamed to clearAllDirty for consistency
    public void clearAllDirty() {
        allDirty = false;
        dirtyItems.clear();
    }

    public void checkForNativeChanges(Player player) {
        if (player.getInventory().getTimesChanged() != lastInventoryVersion) {
            this.allDirty = true;
            lastInventoryVersion = player.getInventory().getTimesChanged();
        }
    }

}