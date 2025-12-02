package com.kjmaster.yield.event;

import com.kjmaster.yield.YieldServices;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

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
        if (player != null) {
            services.sessionController().onTick(player);
        }
    }
}