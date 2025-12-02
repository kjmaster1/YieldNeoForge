package com.kjmaster.yield.service;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface IInventoryProvider {
    /**
     * Collects all items from the provider into the consumer.
     * Implementations MUST return distinct copies (ItemStack.copy()) to ensure thread safety.
     *
     * @param player   The player entity.
     * @param acceptor Consumer to accept copied ItemStacks.
     */
    void collect(Player player, Consumer<ItemStack> acceptor);
}