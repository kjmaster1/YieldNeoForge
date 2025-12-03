package com.kjmaster.yield.api;

import com.kjmaster.yield.project.YieldProject;

import java.util.List;
import java.util.Optional;

public interface IProjectProvider {
    /**
     * Retrieves the currently active project, if one exists.
     *
     * @return An Optional containing the active project, or empty.
     */
    Optional<YieldProject> getActiveProject();

    /**
     * Returns the list of all loaded projects.
     *
     * @return A list of YieldProject.
     */
    List<YieldProject> getProjects();

    /**
     * Checks if the last save attempt failed.
     *
     * @return true if persistence is failing.
     */
    boolean hasSaveFailed();
}