package com.kjmaster.yield.service;

import com.kjmaster.yield.compat.curios.CuriosInventoryProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public class InventoryScanner {

    private final List<IInventoryProvider> providers = new ArrayList<>();

    public InventoryScanner() {
        providers.add(new VanillaInventoryProvider());
        if (ModList.get().isLoaded("curios")) {
            providers.add(new CuriosInventoryProvider());
        }
    }

    /**
     * Creates a thread-safe snapshot of the player's inventory.
     * This method MUST be called on the Main Thread.
     */
    public List<ItemStack> createSnapshot(Player player) {
        List<ItemStack> snapshot = new ArrayList<>();
        // Pre-size optimization could be done if we knew slot counts, but ArrayList resize is fast enough

        for (IInventoryProvider provider : providers) {
            provider.collect(player, snapshot::add);
        }

        return snapshot;
    }
}