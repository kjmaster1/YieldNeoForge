package com.kjmaster.yield;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionController;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.domain.GoalDomainService;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.tracker.InventoryMonitor;
import com.kjmaster.yield.tracker.SessionTracker;

/**
 * Service Registry to hold singleton instances of core services.
 */
public record YieldServices(
        ProjectManager projectManager,
        SessionTracker sessionTracker,
        YieldEventBus eventBus,
        GoalDomainService goalDomainService
) {
    // Convenience Accessors for Interface Segregation
    public IProjectProvider projectProvider() {
        return projectManager;
    }

    public IProjectController projectController() {
        return projectManager;
    }

    public ISessionStatus sessionStatus() {
        return sessionTracker;
    }

    public ISessionController sessionController() {
        return sessionTracker;
    }

    public InventoryMonitor inventoryMonitor() {
        return sessionTracker.getMonitor();
    }
}