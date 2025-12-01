package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.util.StackKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;

public class CuriosScanner {

    public static void scanCurios(Player player, Map<StackKey, Integer> snapshot) {
        // Get the Curios Inventory wrapper
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        // Iterate all equipped curios
        var curiosHandler = curiosInvOpt.get().getEquippedCurios();
        for (int i = 0; i < curiosHandler.getSlots(); i++) {
            ItemStack stack = curiosHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Add the Curio itself (e.g. holding a Totem)
            snapshot.merge(new StackKey(stack), stack.getCount(), Integer::sum);

            // 2. Check INSIDE the Curio (e.g. Backpacks)
            // We check if the equipped item has an ItemHandler capability
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                for (int j = 0; j < internalCap.getSlots(); j++) {
                    ItemStack internalStack = internalCap.getStackInSlot(j);
                    if (!internalStack.isEmpty()) {
                        snapshot.merge(new StackKey(internalStack), internalStack.getCount(), Integer::sum);
                    }
                }
            }
        }
    }
}