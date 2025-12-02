package com.kjmaster.yield.service;

import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Map;

public class VanillaInventoryProvider implements IInventoryProvider {

    @Override
    public void scan(Player player, Map<Item, List<GoalTracker>> itemTrackers, Item targetItemFilter) {
        // 1. Scan Capability Inventory
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ScannerHelper.checkAndIncrement(handler.getStackInSlot(i), itemTrackers, targetItemFilter);
            }
        } else {
            // 2. Vanilla Fallback
            for (ItemStack stack : player.getInventory().items) {
                ScannerHelper.checkAndIncrement(stack, itemTrackers, targetItemFilter);
            }
            for (ItemStack stack : player.getInventory().armor) {
                ScannerHelper.checkAndIncrement(stack, itemTrackers, targetItemFilter);
            }
            for (ItemStack stack : player.getInventory().offhand) {
                ScannerHelper.checkAndIncrement(stack, itemTrackers, targetItemFilter);
            }
        }
    }
}