package com.kjmaster.yield.tracker;

import com.kjmaster.yield.compat.curios.CuriosInventoryWatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

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

        // 2. Capability Strategy (Fix for sync gap)
        // Monitors standard NeoForge ItemHandlers that might wrap vanilla or provide extra slots
        strategies.add(new CapabilityStrategy());

        // 3. Curios Strategy (Conditional)
        if (ModList.get().isLoaded("curios")) {
            strategies.add(new CuriosInventoryWatcher());
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    // Legacy support for event handlers (mapped to general dirty)
    public void markItemDirty(Object item) {
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

    /**
     * Watches the standard ItemHandler capability.
     * Essential for mods that replace/extend the main inventory without updating the vanilla 'timesChanged' counter.
     */
    private static class CapabilityStrategy implements Strategy {
        private long lastStateHash = 0;
        private int tickCounter = 0;

        @Override
        public boolean isDirty(Player player) {

            if (tickCounter++ % 10 != 0) {
                return false;
            }

            IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
            if (handler == null) return false;

            long currentHash = 1;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) {
                    currentHash = 31 * currentHash;
                } else {
                    // Calculate a signature based on Item, Count, and Components
                    long elementHash = System.identityHashCode(stack.getItem());
                    elementHash = 31 * elementHash + stack.getCount();
                    // Using isComponentsPatchEmpty check to be safe/consistent with Curios logic
                    elementHash = 31 * elementHash + (!stack.isComponentsPatchEmpty() ? stack.getComponents().hashCode() : 0);

                    currentHash = 31 * currentHash + elementHash;
                }
            }

            if (currentHash != lastStateHash) {
                lastStateHash = currentHash;
                return true;
            }
            return false;
        }
    }
}