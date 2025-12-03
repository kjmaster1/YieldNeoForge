package com.kjmaster.yield.api;

import net.minecraft.world.entity.player.Player;

public interface ISessionController {
    /**
     * Starts a new tracking session. Resets counters and rates.
     */
    void startSession();

    /**
     * Stops the current tracking session.
     */
    void stopSession();

    /**
     * Marks the inventory state as dirty, forcing a re-scan on the next tick.
     */
    void setDirty();

    /**
     * Tick handler to update tracking logic.
     *
     * @param player The client player entity.
     */
    void onTick(Player player);
}