package com.kjmaster.yield.util;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.world.item.ItemStack;

public class ItemMatcher {
    /**
     * Checks if the source stack matches the target goal.
     * Supports Strict (Components), Tag (Any Item in Tag), and Fuzzy (Item Type) matching.
     */
    public static boolean matches(ItemStack source, ProjectGoal goal) {
        if (source.isEmpty()) return false;

        // 1. Strict Mode: Checks Item Type AND Data Components (Enchants, Name, etc.)
        // Highest priority: If strict is on, we demand an EXACT match.
        if (goal.isStrict()) {
            ItemStack target = goal.getRenderStack();
            if (target.isEmpty()) return false;
            return ItemStack.isSameItemSameComponents(source, target);
        }

        // 2. Tag Mode: Checks if item belongs to the configured Tag
        // If a tag is defined (e.g. #minecraft:logs), match ANY item in that tag.
        if (goal.getTargetTag().isPresent()) {
            return source.is(goal.getTargetTag().get());
        }

        // 3. Fuzzy Mode (Default): Checks Item Type only.
        return source.is(goal.getItem());
    }
}