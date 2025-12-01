package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Map;

public class CuriosScanner {

    public static void scanCurios(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers) {
        // Get the Curios Inventory wrapper
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        // Iterate all equipped curios
        var curiosHandler = curiosInvOpt.get().getEquippedCurios();
        for (int i = 0; i < curiosHandler.getSlots(); i++) {
            ItemStack stack = curiosHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Check the Curio itself
            checkAndIncrement(stack, itemTrackers, tagTrackers);

            // 2. Check INSIDE the Curio (e.g. Backpacks/Satchels equipped in Curios slots)
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                for (int j = 0; j < internalCap.getSlots(); j++) {
                    ItemStack internalStack = internalCap.getStackInSlot(j);
                    if (!internalStack.isEmpty()) {
                        checkAndIncrement(internalStack, itemTrackers, tagTrackers);
                    }
                }
            }
        }
    }

    private static void checkAndIncrement(ItemStack stack, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers) {
        // 1. Fast Lookup: Check Item-Specific Trackers
        List<GoalTracker> specific = itemTrackers.get(stack.getItem());
        if (specific != null) {
            for (GoalTracker tracker : specific) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }

        // 2. Tag Lookup: Check Tag-based goals
        if (tagTrackers != null && !tagTrackers.isEmpty()) {
            for (GoalTracker tracker : tagTrackers) {
                if (ItemMatcher.matches(stack, tracker.getGoal())) {
                    tracker.incrementTempCount(stack.getCount());
                }
            }
        }
    }
}