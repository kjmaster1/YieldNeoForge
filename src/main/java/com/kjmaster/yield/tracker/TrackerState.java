package com.kjmaster.yield.tracker;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerState {
    // Primary Storage
    private final Map<ProjectGoal, GoalTracker> trackers = new ConcurrentHashMap<>();

    // Optimization Caches
    private final Map<Item, List<GoalTracker>> itemSpecificTrackers = new HashMap<>();
    private final List<GoalTracker> tagTrackers = new ArrayList<>();

    private int lastTotalXp = -1;

    public void clear() {
        trackers.clear();
        itemSpecificTrackers.clear();
        tagTrackers.clear();
        lastTotalXp = -1;
    }

    public Map<ProjectGoal, GoalTracker> getTrackers() {
        return trackers;
    }

    public Map<Item, List<GoalTracker>> getItemSpecificTrackers() {
        return itemSpecificTrackers;
    }

    public List<GoalTracker> getTagTrackers() {
        return tagTrackers;
    }

    public int getLastTotalXp() {
        return lastTotalXp;
    }

    public void setLastTotalXp(int lastTotalXp) {
        this.lastTotalXp = lastTotalXp;
    }
}