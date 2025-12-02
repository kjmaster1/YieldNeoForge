package com.kjmaster.yield.api;

import com.kjmaster.yield.project.YieldProject;

public interface IProjectController {
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
     * Updates an existing project state.
     * Since YieldProject is immutable, this replaces the old instance with the new one based on ID.
     */
    void updateProject(YieldProject newProjectState);

    /**
     * Sets the currently active project.
     *
     * @param project The project to activate.
     */
    void setActiveProject(YieldProject project);

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