package com.kjmaster.yield.event;

import com.kjmaster.yield.YieldServices;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

public class YieldLogicHandler {

    private final YieldServices services;
    private boolean pendingLoad = false;

    public YieldLogicHandler(YieldServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pendingLoad = true;
        services.sessionController().stopSession();
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (pendingLoad && event.getEntity() == Minecraft.getInstance().player) {
            services.projectController().load();
            pendingLoad = false;
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        services.projectController().save();
        services.projectController().clear();
        services.sessionController().stopSession();
        pendingLoad = false;
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) services.sessionController().onTick(player);
    }

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            services.sessionController().addXpGain(event.getAmount());
        }
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (isClientPlayer(event.getPlayer())) services.inventoryMonitor().markItemDirty(event.getOriginalStack().getItem());
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (isClientPlayer(event.getPlayer())) services.inventoryMonitor().markItemDirty(event.getEntity().getItem().getItem());
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player p && isClientPlayer(p)) services.inventoryMonitor().markItemDirty(event.getItem().getItem());
    }

    @SubscribeEvent
    public void onItemDestroy(PlayerDestroyItemEvent event) {
        if (isClientPlayer(event.getEntity())) services.inventoryMonitor().markItemDirty(event.getOriginal().getItem());
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (isClientPlayer(event.getEntity())) services.inventoryMonitor().markAllDirty();
    }

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (isClientPlayer(event.getEntity())) services.inventoryMonitor().markItemDirty(event.getSmelting().getItem());
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (isClientPlayer(event.getEntity())) services.inventoryMonitor().markAllDirty();
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (isClientPlayer(event.getEntity())) services.inventoryMonitor().markAllDirty();
    }

    private boolean isClientPlayer(Player p) {
        return p == Minecraft.getInstance().player;
    }
}