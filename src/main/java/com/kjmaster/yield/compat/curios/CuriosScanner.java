package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosScanner {

    public static int countItemsInCurios(Player player, ProjectGoal goal) {
        int count = 0;
        ItemStack targetStack = goal.getRenderStack();

        // Get the Curios Inventory wrapper
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return 0;

        // Iterate all equipped curios
        var curiosHandler = curiosInvOpt.get().getEquippedCurios();
        for (int i = 0; i < curiosHandler.getSlots(); i++) {
            ItemStack stack = curiosHandler.getStackInSlot(i);

            // 1. Check the Curio itself (e.g. holding a Totem)
            if (ItemMatcher.matches(stack, targetStack)) {
                count += stack.getCount();
            }

            // 2. Check INSIDE the Curio (e.g. Backpacks)
            // We check if the equipped item has an ItemHandler capability
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                for (int j = 0; j < internalCap.getSlots(); j++) {
                    ItemStack internalStack = internalCap.getStackInSlot(j);
                    if (ItemMatcher.matches(internalStack, targetStack)) {
                        count += internalStack.getCount();
                    }
                }
            }
        }
        return count;
    }
}