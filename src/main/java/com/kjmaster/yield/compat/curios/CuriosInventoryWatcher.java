package com.kjmaster.yield.compat.curios;

import com.kjmaster.yield.tracker.InventoryMonitor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosInventoryWatcher implements InventoryMonitor.Strategy {

    private long lastStateHash = 0;
    private int tickCounter = 0;

    @Override
    public boolean isDirty(Player player) {

        if (tickCounter++ % 10 != 0) {
            return false;
        }

        long currentHash = calculateCuriosHash(player);
        if (currentHash != lastStateHash) {
            lastStateHash = currentHash;
            return true;
        }
        return false;
    }

    private long calculateCuriosHash(Player player) {
        var curiosInvOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInvOpt.isEmpty()) return 0;

        IItemHandler handler = curiosInvOpt.get().getEquippedCurios();
        long hash = 1;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                hash = 31 * hash;
            } else {
                // Calculate a signature based on Item, Count, and Components
                // This detects swaps, consumption, and strict-mode relevant changes
                long elementHash = System.identityHashCode(stack.getItem());
                elementHash = 31 * elementHash + stack.getCount();
                elementHash = 31 * elementHash + (!stack.isComponentsPatchEmpty() ? stack.getComponents().hashCode() : 0);

                hash = 31 * hash + elementHash;
            }
        }
        return hash;
    }
}