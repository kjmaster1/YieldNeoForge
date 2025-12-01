package com.kjmaster.yield.api;

import com.kjmaster.yield.project.YieldProject;

import java.util.List;
import java.util.Optional;

public interface IProjectManager {
    /**
     * Creates a new project with the given name and saves it.
     *
     * @param name The name of the new project.
     */
    void createProject(String name);

    /**
     * Deletes the specified project and clears it if it is active.
     *
     * @param project The project to delete.
     */
    void deleteProject(YieldProject project);

    /**
     * Sets the currently active project.
     *
     * @param project The project to activate.
     */
    void setActiveProject(YieldProject project);

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
     * Clears all loaded projects and active state.
     */
    void clear();

    /**
     * Persists the current state of projects to disk.
     */
    void save();

    /**
     * Loads projects from disk.
     */
    void load();
}