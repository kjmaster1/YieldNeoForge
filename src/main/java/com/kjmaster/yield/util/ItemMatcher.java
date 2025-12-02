package com.kjmaster.yield.util;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ItemMatcher {
    /**
     * Checks if the source stack matches the target goal.
     * Supports Smart Strict (Components ignoring Damage), Tag, and Fuzzy (Item Type) matching.
     */
    public static boolean matches(ItemStack source, ProjectGoal goal) {
        if (source.isEmpty()) return false;

        // 1. Tag Mode: Checks if item belongs to the configured Tag
        // Check this first as it encompasses multiple Item Types
        if (goal.targetTag().isPresent()) {
            return source.is(goal.targetTag().get());
        }

        // 2. Base Item Check (Required for both Fuzzy and Strict)
        if (source.getItem() != goal.item()) {
            return false;
        }

        // 3. Strict Mode: Checks Data Components
        if (goal.strict()) {
            return areComponentsEqualIgnoringDamage(source, goal.getRenderStack());
        }

        // 4. Fuzzy Mode: Item Type matched above
        return true;
    }

    /**
     * Compares components of two stacks but ignores the DAMAGE (Durability) component.
     */
    private static boolean areComponentsEqualIgnoringDamage(ItemStack a, ItemStack b) {
        // If exact match (including damage), return true immediately (Fast path)
        if (ItemStack.isSameItemSameComponents(a, b)) return true;

        // Iterate over components of A and check against B
        // Note: This is a bidirectional check to ensure B doesn't have extra components A lacks
        return checkContains(a, b) && checkContains(b, a);
    }

    private static boolean checkContains(ItemStack stackA, ItemStack stackB) {
        for (TypedDataComponent<?> component : stackA.getComponents()) {
            DataComponentType<?> type = component.type();

            // IGNORE Durability/Damage
            if (type == DataComponents.DAMAGE) continue;

            // Check if B has this component and if values are equal
            if (!stackB.has(type)) return false;

            Object valueA = component.value();
            Object valueB = stackB.get(type);

            if (!Objects.equals(valueA, valueB)) return false;
        }
        return true;
    }
}