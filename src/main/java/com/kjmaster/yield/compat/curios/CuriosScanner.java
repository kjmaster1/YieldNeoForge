package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Collection;

public class CuriosScanner {

    public static void scanCurios(Player player, Collection<GoalTracker> activeTrackers) {
        // Get the Curios Inventory wrapper
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        // Iterate all equipped curios
        var curiosHandler = curiosInvOpt.get().getEquippedCurios();
        for (int i = 0; i < curiosHandler.getSlots(); i++) {
            ItemStack stack = curiosHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Check the Curio itself
            checkAndIncrement(stack, activeTrackers);

            // 2. Check INSIDE the Curio (e.g. Backpacks)
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                for (int j = 0; j < internalCap.getSlots(); j++) {
                    ItemStack internalStack = internalCap.getStackInSlot(j);
                    if (!internalStack.isEmpty()) {
                        checkAndIncrement(internalStack, activeTrackers);
                    }
                }
            }
        }
    }

    private static void checkAndIncrement(ItemStack stack, Collection<GoalTracker> activeTrackers) {
        // O(Goals) check - usually very fast as active goals are few (<10)
        for (GoalTracker tracker : activeTrackers) {
            if (ItemMatcher.matches(stack, tracker.getGoal())) {
                tracker.incrementTempCount(stack.getCount());
            }
        }
    }
}