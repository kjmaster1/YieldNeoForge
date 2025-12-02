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
     * Adds an XP gain amount to the XP rate calculator.
     *
     * @param amount The amount of XP gained.
     */
    void addXpGain(int amount);

    /**
     * Tick handler to update tracking logic.
     *
     * @param player The client player entity.
     */
    void onTick(Player player);
}