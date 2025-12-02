package com.kjmaster.yield.event;

import com.kjmaster.yield.api.ISessionController;
import com.kjmaster.yield.tracker.InventoryMonitor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Event bridge that subscribes to NeoForge events and translates them into
 * simple state changes for the core tracking logic.
 * This decouples the InventoryMonitor and SessionTracker from the NeoForge bus.
 */
public class YieldNeoForgeEventHandler {

    private final ISessionController sessionController;
    private final InventoryMonitor inventoryMonitor;

    public YieldNeoForgeEventHandler(ISessionController sessionController, InventoryMonitor inventoryMonitor) {
        this.sessionController = sessionController;
        this.inventoryMonitor = inventoryMonitor;
    }

    // --- Events Handling Tracking Tick ---

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // Delegation of the tracking update logic
            sessionController.onTick(player);
        }
    }

    // --- Events Handling Inventory Monitor State ---

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (isClientPlayer(event.getPlayer())) {
            inventoryMonitor.markItemDirty(event.getOriginalStack().getItem());
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (isClientPlayer(event.getPlayer())) {
            inventoryMonitor.markItemDirty(event.getEntity().getItem().getItem());
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player p && isClientPlayer(p)) {
            inventoryMonitor.markItemDirty(event.getItem().getItem());
        }
    }

    @SubscribeEvent
    public void onItemDestroy(PlayerDestroyItemEvent event) {
        if (isClientPlayer(event.getEntity())) {
            inventoryMonitor.markItemDirty(event.getOriginal().getItem());
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (isClientPlayer(event.getEntity())) {
            // Crafting affects multiple items (inputs/outputs), safer to flag all
            inventoryMonitor.markAllDirty();
        }
    }

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (isClientPlayer(event.getEntity())) {
            inventoryMonitor.markItemDirty(event.getSmelting().getItem());
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (isClientPlayer(event.getEntity())) inventoryMonitor.markAllDirty();
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (isClientPlayer(event.getEntity())) inventoryMonitor.markAllDirty();
    }

    private boolean isClientPlayer(Player p) {
        return p == Minecraft.getInstance().player;
    }
}