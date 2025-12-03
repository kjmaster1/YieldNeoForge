package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.service.IInventoryProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CuriosInventoryProvider implements IInventoryProvider {

    private static final int MAX_DEPTH = 1; // Safety limit to prevent StackOverflow

    @Override
    public void collect(Player player, Consumer<ItemStack> acceptor) {
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        var curiosHandler = curiosInvOpt.get().getEquippedCurios();

        // Use Identity Hash Set to prevent circular recursion logic
        Set<Object> visitedHandlers = new HashSet<>();
        // Start scanning at depth 0
        scanHandler(curiosHandler, acceptor, visitedHandlers, 0);
    }

    private void scanHandler(IItemHandler handler, Consumer<ItemStack> acceptor, Set<Object> visited, int depth) {
        // Recursion Guard: Stop if we go too deep
        if (depth > MAX_DEPTH) return;

        // If we've already seen this exact handler instance, abort to prevent cycles
        if (!visited.add(handler)) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Collect the item itself
            acceptor.accept(stack.copy());

            // 2. Check for nested inventory capability (Backpacks, etc.)
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                // Recursive call with incremented depth
                scanHandler(internalCap, acceptor, visited, depth + 1);
            }
        }
    }
}