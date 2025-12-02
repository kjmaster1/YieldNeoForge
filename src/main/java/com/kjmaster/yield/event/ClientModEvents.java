package com.kjmaster.yield.event;

import com.kjmaster.yield.Yield;
import com.kjmaster.yield.client.KeyBindings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Yield.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_DASHBOARD);
        event.register(KeyBindings.QUICK_TRACK);
        event.register(KeyBindings.TOGGLE_OVERLAY);
    }

}