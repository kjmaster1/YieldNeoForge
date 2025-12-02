package com.kjmaster.yield.tracker;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackerState {
    // Replaced ConcurrentHashMap with HashMap for performance on the client thread.
    // Logic is bound to the render/tick thread, so synchronization is unnecessary overhead.
    private final Map<UUID, GoalTracker> trackers = new HashMap<>();

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