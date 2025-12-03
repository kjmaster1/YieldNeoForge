package com.kjmaster.yield.util;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ItemMatcher {
    /**
     * Checks if the source stack matches the target goal.
     * Supports Tag, Strict (Masked), and Fuzzy matching.
     */
    public static boolean matches(ItemStack source, ProjectGoal goal) {
        if (source.isEmpty()) return false;

        // 1. Tag Mode
        if (goal.targetTag().isPresent()) {
            return source.is(goal.targetTag().get());
        }

        // 2. Base Item Check
        if (source.getItem() != goal.item()) {
            return false;
        }

        // 3. Strict Mode (Masked)
        if (goal.strict()) {
            return areComponentsEqualMasked(source, goal.getRenderStack(), goal.ignoredComponents());
        }

        // 4. Fuzzy Mode
        return true;
    }

    private static boolean areComponentsEqualMasked(ItemStack a, ItemStack b, List<ResourceLocation> ignoredIds) {
        // Fast path: Exact reference or full value equality (if no mask)
        if (ignoredIds.isEmpty() && ItemStack.isSameItemSameComponents(a, b)) return true;

        // Resolve ResourceLocations to ComponentTypes for faster lookup
        Set<DataComponentType<?>> ignoredTypes = ignoredIds.stream()
                .map(BuiltInRegistries.DATA_COMPONENT_TYPE::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return checkContains(a, b, ignoredTypes) && checkContains(b, a, ignoredTypes);
    }

    public static boolean checkContains(ItemStack stackA, ItemStack stackB, Set<DataComponentType<?>> ignored) {
        for (TypedDataComponent<?> component : stackA.getComponents()) {
            DataComponentType<?> type = component.type();

            if (ignored.contains(type)) continue;

            // Check if B has this component
            if (!stackB.has(type)) return false;

            // Value equality
            Object valueA = component.value();
            Object valueB = stackB.get(type);

            if (!Objects.equals(valueA, valueB)) return false;
        }
        return true;
    }
}