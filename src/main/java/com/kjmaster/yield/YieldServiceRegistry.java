package com.kjmaster.yield;

import com.kjmaster.yield.api.IProjectManager;
import com.kjmaster.yield.api.ISessionTracker;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.manager.ProjectRepository;
import com.kjmaster.yield.tracker.SessionTracker;

/**
 * Service Registry (Service Locator) for Yield.
 * Handles the instantiation and retrieval of core services.
 */
public class YieldServiceRegistry {

    private static IProjectManager projectManager;
    private static ISessionTracker sessionTracker;

    /**
     * Initializes the services. Should be called during Mod construction or Client Setup.
     */
    public static void init() {
        if (projectManager != null) {
            return; // Already initialized
        }

        // 1. Instantiate Persistence Layer
        ProjectRepository repository = new ProjectRepository();

        // 2. Instantiate ProjectManager (Injects Repository)
        projectManager = new ProjectManager(repository);

        // 3. Instantiate SessionTracker (Injects ProjectManager)
        sessionTracker = new SessionTracker(projectManager);

        Yield.LOGGER.info("Yield Services Initialized.");
    }

    public static IProjectManager getProjectManager() {
        if (projectManager == null) {
            throw new IllegalStateException("Yield Service Registry not initialized!");
        }
        return projectManager;
    }

    public static ISessionTracker getSessionTracker() {
        if (sessionTracker == null) {
            throw new IllegalStateException("Yield Service Registry not initialized!");
        }
        return sessionTracker;
    }
}