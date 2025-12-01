package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.service.IInventoryProvider;
import com.kjmaster.yield.service.ScannerHelper;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Map;

public class CuriosInventoryProvider implements IInventoryProvider {

    @Override
    public void scan(Player player, Map<Item, List<GoalTracker>> itemTrackers, List<GoalTracker> tagTrackers, Item targetItemFilter) {
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        var curiosHandler = curiosInvOpt.get().getEquippedCurios();
        for (int i = 0; i < curiosHandler.getSlots(); i++) {
            ItemStack stack = curiosHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Check the Curio itself
            ScannerHelper.checkAndIncrement(stack, itemTrackers, tagTrackers, targetItemFilter);

            // 2. Check INSIDE the Curio (e.g. Backpacks/Satchels equipped in Curios slots)
            IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (internalCap != null) {
                for (int j = 0; j < internalCap.getSlots(); j++) {
                    ItemStack internalStack = internalCap.getStackInSlot(j);
                    if (!internalStack.isEmpty()) {
                        ScannerHelper.checkAndIncrement(internalStack, itemTrackers, tagTrackers, targetItemFilter);
                    }
                }
            }
        }
    }
}