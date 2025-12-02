package com.kjmaster.yield.tracker;

import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionController;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.event.internal.YieldEvents;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class SessionTracker implements ISessionStatus, ISessionController {

    private final IProjectProvider projectProvider;
    private final YieldEventBus eventBus;

    // Components
    private final TrackerState state;
    private final InventoryMonitor monitor;
    private final TrackerEngine engine;

    private boolean isRunning = false;
    private long sessionStartTime = 0;

    public SessionTracker(IProjectProvider projectProvider, YieldEventBus eventBus) {
        this.projectProvider = projectProvider;
        this.eventBus = eventBus;
        this.state = new TrackerState();
        this.monitor = new InventoryMonitor();
        this.engine = new TrackerEngine(state, monitor);
    }

    public InventoryMonitor getMonitor() {
        return monitor;
    }

    @Override
    public void startSession() {
        if (isRunning) return;
        isRunning = true;
        sessionStartTime = System.currentTimeMillis();

        state.clear();
        engine.reset();

        monitor.clearDirty();
        monitor.markAllDirty(); // Force initial scan
        eventBus.post(new YieldEvents.SessionStarted());
    }

    @Override
    public void stopSession() {
        if (!isRunning) return;
        isRunning = false;
        eventBus.post(new YieldEvents.SessionStopped());
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setDirty() {
        monitor.markAllDirty();
    }

    @Override
    public long getSessionDuration() {
        if (!isRunning) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    @Override
    public GoalTracker getTracker(ProjectGoal goal) {
        // 1. Look up by UUID (Persistent Identity)
        GoalTracker tracker = state.getTrackers().computeIfAbsent(
                goal.id(),
                uuid -> new GoalTracker(goal, engine.getTimeSource())
        );

        // 2. Sync Definition
        tracker.updateGoalDefinition(goal);

        return tracker;
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
        Optional<YieldProject> projectOpt = projectProvider.getActiveProject();
        if (projectOpt.isEmpty()) return;
        engine.onTick(player, projectOpt.get());
    }
}