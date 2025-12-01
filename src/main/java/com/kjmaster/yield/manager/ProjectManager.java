package com.kjmaster.yield.manager;

import com.kjmaster.yield.api.IProjectManager;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.util.Debouncer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ProjectManager implements IProjectManager {

    private final ProjectRepository repository;
    private final Debouncer debouncer;

    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

    /**
     * Constructor Injection
     */
    public ProjectManager(ProjectRepository repository) {
        this.repository = repository;
        this.debouncer = new Debouncer();
    }

    @Override
    public void createProject(String name) {
        YieldProject project = new YieldProject(name);
        projects.add(project);
        save();
    }

    @Override
    public void deleteProject(YieldProject project) {
        projects.remove(project);
        if (activeProject == project) activeProject = null;
        save();
    }

    @Override
    public void setActiveProject(YieldProject project) {
        this.activeProject = project;
        // Setting active project might not need a save unless we persist "Last Active" state.
        // If we do want to persist it, we would call save() here.
    }

    @Override
    public Optional<YieldProject> getActiveProject() {
        return Optional.ofNullable(activeProject);
    }

    @Override
    public List<YieldProject> getProjects() {
        return projects;
    }

    @Override
    public void clear() {
        this.projects.clear();
        this.activeProject = null;
    }

    @Override
    public void save() {
        // Debounce the save operation: Wait 2 seconds of inactivity
        // We create a snapshot of the list to ensure thread safety during the delayed write
        List<YieldProject> snapshot = new ArrayList<>(this.projects);

        debouncer.debounce(() -> {
            repository.save(snapshot);
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public void load() {
        this.projects.clear();
        this.activeProject = null;

        // Load is blocking/immediate because it usually happens on startup/login
        List<YieldProject> loaded = repository.load();
        this.projects.addAll(loaded);
    }
}