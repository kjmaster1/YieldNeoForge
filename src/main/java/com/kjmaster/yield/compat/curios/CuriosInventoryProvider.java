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

    // Limit recursion to avoid lag spikes with nested backpacks
    private static final int MAX_DEPTH = 2;

    @Override
    public void scan(Player player, Map<Item, List<GoalTracker>> itemTrackers, Item targetItemFilter) {
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return;

        var curiosHandler = curiosInvOpt.get().getEquippedCurios();

        scanHandler(curiosHandler, itemTrackers, targetItemFilter, 0);
    }

    private void scanHandler(IItemHandler handler, Map<Item, List<GoalTracker>> itemTrackers, Item targetItemFilter, int currentDepth) {
        if (currentDepth > MAX_DEPTH) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1. Scan the item itself
            ScannerHelper.checkAndIncrement(stack, itemTrackers, targetItemFilter);

            // 2. Check for nested inventory capability (Backpacks, etc.)
            // Only proceed if we haven't hit the depth limit
            if (currentDepth < MAX_DEPTH) {
                IItemHandler internalCap = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
                if (internalCap != null) {
                    scanHandler(internalCap, itemTrackers, targetItemFilter, currentDepth + 1);
                }
            }
        }
    }
}