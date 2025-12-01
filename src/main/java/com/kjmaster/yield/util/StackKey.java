package com.kjmaster.yield.util;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Wrapper to allow ItemStack to be used as a Map key.
 * Uses strict component equality (isSameItemSameComponents).
 */
public record StackKey(ItemStack stack) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackKey stackKey = (StackKey) o;
        // Delegate to Vanilla's strict check
        return ItemStack.isSameItemSameComponents(this.stack, stackKey.stack);
    }

    @Override
    public int hashCode() {
        // Combine Item Hash and Components Hash
        return Objects.hash(stack.getItem(), stack.getComponents());
    }
}