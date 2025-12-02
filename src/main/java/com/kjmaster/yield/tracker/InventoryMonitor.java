package com.kjmaster.yield.tracker;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

public class InventoryMonitor {

    private boolean allDirty = true;
    private final Set<Item> dirtyItems = new HashSet<>();
    private int lastInventoryVersion = -1;

    public void markAllDirty() {
        this.allDirty = true;
        // If everything is dirty, we don't need to track specifics
        this.dirtyItems.clear();
    }

    public void markItemDirty(Item item) {
        // If we are already doing a full scan, ignore granular updates
        if (!allDirty) {
            this.dirtyItems.add(item);
        }
    }

    public boolean isAllDirty() {
        return allDirty;
    }

    public Set<Item> getDirtyItems() {
        return dirtyItems;
    }

    public void clearAllDirty() {
        allDirty = false;
        dirtyItems.clear();
    }

    public void checkForNativeChanges(Player player) {
        // Minecraft's inventory revision counter increments on ANY change.
        // If this mismatches, we must assume a full scan is needed because
        // we don't know *what* changed (could be a swap, a move, etc that events didn't catch).
        int currentVersion = player.getInventory().getTimesChanged();
        if (currentVersion != lastInventoryVersion) {
            this.allDirty = true;
            this.dirtyItems.clear(); // Optimization: clear granular set
            lastInventoryVersion = currentVersion;
        }
    }
}