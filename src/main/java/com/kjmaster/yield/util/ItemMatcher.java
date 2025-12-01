package com.kjmaster.yield.util;

import net.minecraft.world.item.ItemStack;

public class ItemMatcher {
    /**
     * Checks if the source stack matches the target goal.
     * Currently, performs a "Fuzzy" match (Item Type only), ignoring Data Components.
     */
    public static boolean matches(ItemStack source, ItemStack target) {
        if (source.isEmpty() || target.isEmpty()) return false;

        // 1.21.1 Standard: Checks if the item definition matches.
        // This effectively ignores Data Components (Damage, Enchants, etc.)
        return source.is(target.getItem());
    }
}
