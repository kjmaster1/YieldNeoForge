package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosScanner;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.util.ItemMatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class InventoryScanner {

    private final boolean isCuriosLoaded;

    public InventoryScanner() {
        this.isCuriosLoaded = ModList.get().isLoaded("curios");
    }

    public int countItem(Player player, ProjectGoal goal) {
        ItemStack targetStack = goal.getRenderStack();
        int count = 0;

        // 1. Scan Player Inventory (Main + Offhand + Armor)
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (ItemMatcher.matches(stack, targetStack)) {
                    count += stack.getCount();
                }
            }
        } else {
            // Fallback for vanilla inventory
            for (ItemStack stack : player.getInventory().items) {
                if (ItemMatcher.matches(stack, targetStack)) count += stack.getCount();
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (ItemMatcher.matches(stack, targetStack)) count += stack.getCount();
            }
        }

        // 2. Scan Curios (Backpacks/Accessories)
        if (isCuriosLoaded) {
            count += CuriosScanner.countItemsInCurios(player, goal);
        }

        return count;
    }
}