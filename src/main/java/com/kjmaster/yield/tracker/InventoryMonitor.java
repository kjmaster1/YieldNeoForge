package com.kjmaster.yield.tracker;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;

public class InventoryMonitor {

    private boolean allDirty = true;
    private final Set<Item> dirtyItems = new HashSet<>();
    private int lastInventoryVersion = -1;

    public void register() {
        NeoForge.EVENT_BUS.register(this);
    }

    public void unregister() {
        NeoForge.EVENT_BUS.unregister(this);
    }

    public void setAllDirty() {
        this.allDirty = true;
    }

    public boolean isAllDirty() {
        return allDirty;
    }

    public Set<Item> getDirtyItems() {
        return dirtyItems;
    }

    public void clearDirty() {
        allDirty = false;
        dirtyItems.clear();
    }

    public void checkForNativeChanges(Player player) {
        if (player.getInventory().getTimesChanged() != lastInventoryVersion) {
            this.allDirty = true;
            lastInventoryVersion = player.getInventory().getTimesChanged();
        }
    }

    // --- Event Listeners ---

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (isClientPlayer(event.getPlayer())) {
            dirtyItems.add(event.getOriginalStack().getItem());
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (isClientPlayer(event.getPlayer())) {
            dirtyItems.add(event.getEntity().getItem().getItem());
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player p && isClientPlayer(p)) {
            dirtyItems.add(event.getItem().getItem());
        }
    }

    @SubscribeEvent
    public void onItemDestroy(PlayerDestroyItemEvent event) {
        if (isClientPlayer(event.getEntity())) {
            dirtyItems.add(event.getOriginal().getItem());
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (isClientPlayer(event.getEntity())) {
            // Crafting affects multiple items (inputs/outputs), simpler to flag all
            allDirty = true;
        }
    }

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (isClientPlayer(event.getEntity())) {
            dirtyItems.add(event.getSmelting().getItem());
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (isClientPlayer(event.getEntity())) allDirty = true;
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (isClientPlayer(event.getEntity())) allDirty = true;
    }

    private boolean isClientPlayer(Player p) {
        return p == Minecraft.getInstance().player;
    }
}