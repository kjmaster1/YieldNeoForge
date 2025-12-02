package com.kjmaster.yield.tracker;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerState {
    // Map UUID (Goal ID) -> Tracker
    // This allows the ProjectGoal record to change (e.g., amount, strictness) without losing the tracker.
    private final Map<UUID, GoalTracker> trackers = new ConcurrentHashMap<>();

    // Optimization Caches
    private final Map<Item, List<GoalTracker>> itemSpecificTrackers = new HashMap<>();

    private int lastTotalXp = -1;

    public void clear() {
        trackers.clear();
        itemSpecificTrackers.clear();
        lastTotalXp = -1;
    }

    public Map<UUID, GoalTracker> getTrackers() {
        return trackers;
    }

    public Map<Item, List<GoalTracker>> getItemSpecificTrackers() {
        return itemSpecificTrackers;
    }

    public int getLastTotalXp() {
        return lastTotalXp;
    }

    public void setLastTotalXp(int lastTotalXp) {
        this.lastTotalXp = lastTotalXp;
    }
}