package com.kjmaster.yield.service;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VanillaInventoryProvider implements IInventoryProvider {

    private static final int MAX_DEPTH = 8; // Safety limit

    @Override
    public void collect(Player player, Consumer<ItemStack> acceptor) {
        Set<Object> visited = new HashSet<>();
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            scanHandler(handler, acceptor, visited, 0); // Start at depth 0
        } else {
            scanList(player.getInventory().items, acceptor, visited, 0);
            scanList(player.getInventory().armor, acceptor, visited, 0);
            scanList(player.getInventory().offhand, acceptor, visited, 0);
        }
    }

    private void scanList(Iterable<ItemStack> list, Consumer<ItemStack> acceptor, Set<Object> visited, int depth) {
        for (ItemStack stack : list) {
            processStack(stack, acceptor, visited, depth);
        }
    }

    private void scanHandler(IItemHandler handler, Consumer<ItemStack> acceptor, Set<Object> visited, int depth) {
        if (depth > MAX_DEPTH) return; // Recursion Guard
        if (!visited.add(handler)) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            processStack(stack, acceptor, visited, depth);
        }
    }

    private void processStack(ItemStack stack, Consumer<ItemStack> acceptor, Set<Object> visited, int depth) {
        if (stack.isEmpty()) return;

        acceptor.accept(stack.copy());

        IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
        if (internalCap != null) {
            scanHandler(internalCap, acceptor, visited, depth + 1); // Increment depth
        }
    }
}