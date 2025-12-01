package com.kjmaster.yield.util;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.world.item.ItemStack;

public class ItemMatcher {
    /**
     * Checks if the source stack matches the target goal.
     * Supports both Fuzzy (Item Type) and Strict (Components/NBT) matching.
     */
    public static boolean matches(ItemStack source, ProjectGoal goal) {
        if (source.isEmpty()) return false;
        ItemStack target = goal.getRenderStack();
        if (target.isEmpty()) return false;

        if (goal.isStrict()) {
            // Strict Mode: Checks Item Type AND Data Components (Enchants, Name, etc.)
            // Ignores stack size.
            return ItemStack.isSameItemSameComponents(source, target);
        } else {
            // Fuzzy Mode (Default): Checks Item Type only.
            return source.is(target.getItem());
        }
    }
}