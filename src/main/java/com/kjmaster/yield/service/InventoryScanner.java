package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosScanner;
import com.kjmaster.yield.util.StackKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;

public class InventoryScanner {

    private final boolean isCuriosLoaded;

    public InventoryScanner() {
        this.isCuriosLoaded = ModList.get().isLoaded("curios");
    }

    /**
     * Scans inventory and returns a map of StackKey -> Total Count.
     * This preserves Component data for Strict Mode matching.
     */
    public Map<StackKey, Integer> scanInventory(Player player) {
        Map<StackKey, Integer> snapshot = new HashMap<>();

        // 1. Scan Capability Inventory
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                addToSnapshot(snapshot, handler.getStackInSlot(i));
            }
        } else {
            // Vanilla Fallback
            for (ItemStack stack : player.getInventory().items) addToSnapshot(snapshot, stack);
            for (ItemStack stack : player.getInventory().armor) addToSnapshot(snapshot, stack);
            for (ItemStack stack : player.getInventory().offhand) addToSnapshot(snapshot, stack);
        }

        // 2. Scan Curios
        if (isCuriosLoaded) {
            // Note: CuriosScanner needs slight refactor to accept Map<StackKey, Integer>
            // For now, inlining simple Curios scan to support the new Map type
            CuriosScanner.scanCurios(player, snapshot);
        }

        return snapshot;
    }

    private void addToSnapshot(Map<StackKey, Integer> snapshot, ItemStack stack) {
        if (!stack.isEmpty()) {
            // Wrap in StackKey for component-aware hashing
            snapshot.merge(new StackKey(stack), stack.getCount(), Integer::sum);
        }
    }
}