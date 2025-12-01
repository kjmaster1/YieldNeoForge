package com.kjmaster.yield.time;

import net.minecraft.client.Minecraft;

public class GameTickSource implements TimeSource {
    private long startTick = 0;

    @Override
    public double getTimeSeconds() {
        if (Minecraft.getInstance().level == null) return 0;
        // 20 ticks per second
        long current = Minecraft.getInstance().level.getGameTime();
        return (current - startTick) / 20.0;
    }

    @Override
    public void reset() {
        if (Minecraft.getInstance().level != null) {
            this.startTick = Minecraft.getInstance().level.getGameTime();
        }
    }
}