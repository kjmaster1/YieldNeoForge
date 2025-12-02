package com.kjmaster.yield.service;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Consumer;

public class VanillaInventoryProvider implements IInventoryProvider {

    @Override
    public void collect(Player player, Consumer<ItemStack> acceptor) {
        // 1. Scan Capability Inventory (Modern NeoForge approach for player inv)
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);

        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    acceptor.accept(stack.copy());
                }
            }
        } else {
            // 2. Vanilla Fallback
            collectFromList(player.getInventory().items, acceptor);
            collectFromList(player.getInventory().armor, acceptor);
            collectFromList(player.getInventory().offhand, acceptor);
        }
    }

    private void collectFromList(Iterable<ItemStack> list, Consumer<ItemStack> acceptor) {
        for (ItemStack stack : list) {
            if (!stack.isEmpty()) {
                acceptor.accept(stack.copy());
            }
        }
    }
}