package com.kjmaster.yield.tracker;

import com.kjmaster.yield.api.IProjectManager;
import com.kjmaster.yield.api.ISessionTracker;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class SessionTracker implements ISessionTracker {

    private final IProjectManager projectManager;

    // Components
    private final TrackerState state;
    private final InventoryMonitor monitor;
    private final TrackerEngine engine;

    private boolean isRunning = false;
    private long sessionStartTime = 0;

    public SessionTracker(IProjectManager projectManager) {
        this.projectManager = projectManager;
        this.state = new TrackerState();
        this.monitor = new InventoryMonitor();
        this.engine = new TrackerEngine(state, monitor);
    }

    @Override
    public void startSession() {
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();

        state.clear();
        engine.reset();

        monitor.clearDirty();
        monitor.setAllDirty(); // Force initial scan
        monitor.register();    // Start listening to events
    }

    @Override
    public void stopSession() {
        isRunning = false;
        monitor.unregister();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setDirty() {
        monitor.setAllDirty();
    }

    @Override
    public long getSessionDuration() {
        if (!isRunning) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    @Override
    public GoalTracker getTracker(ProjectGoal goal) {
        // If not present, create it. Engine will sync it later, or we create it here.
        return state.getTrackers().computeIfAbsent(goal, g -> new GoalTracker(g, engine.getTimeSource()));
    }

    @Override
    public void addXpGain(int amount) {
        if (!isRunning) return;
        engine.addXp(amount);
    }

    @Override
    public double getXpPerHour() {
        return engine.getXpRate();
    }

    @Override
    public void onTick(Player player) {
        if (!isRunning || player == null) return;

        Optional<YieldProject> projectOpt = projectManager.getActiveProject();
        if (projectOpt.isEmpty()) return;

        engine.onTick(player, projectOpt.get());
    }
}