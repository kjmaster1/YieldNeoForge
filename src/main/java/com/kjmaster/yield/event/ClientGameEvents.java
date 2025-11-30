package com.kjmaster.yield.event;

import com.kjmaster.yield.Yield;
import com.kjmaster.yield.client.KeyBindings;
import com.kjmaster.yield.client.screen.YieldDashboardScreen;
import com.kjmaster.yield.tracker.SessionTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Yield.MODID, value = Dist.CLIENT)
public class ClientGameEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Safety Checks
        if (mc.player == null || mc.level == null) return;

        // 2. Logic: Tracker Update
        SessionTracker.get().onTick(mc.player);

        // 3. Logic: Input Handling
        // consumeClick() is the standard way to check "Did the user press this binding?"
        // inside the game loop safely.
        while (KeyBindings.OPEN_DASHBOARD.consumeClick()) {
            // Only open if a screen isn't already open (optional, but good UX)
            // or if you want it to close the current screen, standard behavior is usually:
            if (mc.screen == null) {
                mc.setScreen(new YieldDashboardScreen());
            } else if (mc.screen instanceof YieldDashboardScreen) {
                mc.setScreen(null); // Toggle closed if already open
            }
        }
    }
}