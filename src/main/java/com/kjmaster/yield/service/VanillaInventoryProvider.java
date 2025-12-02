package com.kjmaster.yield.service;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VanillaInventoryProvider implements IInventoryProvider {

    @Override
    public void collect(Player player, Consumer<ItemStack> acceptor) {
        // Track visited handlers to prevent infinite recursion
        Set<Object> visited = new HashSet<>();

        // 1. Scan Capability Inventory (Modern NeoForge approach for player inv)
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            scanHandler(handler, acceptor, visited);
        } else {
            // 2. Vanilla Fallback
            scanList(player.getInventory().items, acceptor, visited);
            scanList(player.getInventory().armor, acceptor, visited);
            scanList(player.getInventory().offhand, acceptor, visited);
        }
    }

    private void scanList(Iterable<ItemStack> list, Consumer<ItemStack> acceptor, Set<Object> visited) {
        for (ItemStack stack : list) {
            processStack(stack, acceptor, visited);
        }
    }

    private void scanHandler(IItemHandler handler, Consumer<ItemStack> acceptor, Set<Object> visited) {
        // If we've already seen this exact handler instance, abort to prevent cycles
        if (!visited.add(handler)) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            processStack(stack, acceptor, visited);
        }
    }

    private void processStack(ItemStack stack, Consumer<ItemStack> acceptor, Set<Object> visited) {
        if (stack.isEmpty()) return;

        // 1. Collect the item itself
        acceptor.accept(stack.copy());

        // 2. Check for nested inventory capability (Backpacks, etc.)
        IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
        if (internalCap != null) {
            scanHandler(internalCap, acceptor, visited);
        }
    }
}