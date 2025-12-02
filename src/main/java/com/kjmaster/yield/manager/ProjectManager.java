package com.kjmaster.yield.manager;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.util.Debouncer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ProjectManager implements IProjectProvider, IProjectController {

    private final ProjectRepository repository;
    private final Debouncer debouncer;

    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

    // Volatile to ensure visibility across threads (Debouncer -> Render Thread)
    private volatile boolean saveFailed = false;

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
        projects.removeIf(p -> p.id().equals(project.id()));
        if (activeProject != null && activeProject.id().equals(project.id())) {
            activeProject = null;
        }
        save();
    }

    @Override
    public void updateProject(YieldProject newProjectState) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).id().equals(newProjectState.id())) {
                projects.set(i, newProjectState);
                if (activeProject != null && activeProject.id().equals(newProjectState.id())) {
                    activeProject = newProjectState;
                }
                save();
                return;
            }
        }
    }

    @Override
    public void setActiveProject(YieldProject project) {
        this.activeProject = project;
    }

    @Override
    public Optional<YieldProject> getActiveProject() {
        return Optional.ofNullable(activeProject);
    }

    @Override
    public List<YieldProject> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    @Override
    public boolean hasSaveFailed() {
        return saveFailed;
    }

    @Override
    public void clear() {
        this.projects.clear();
        this.activeProject = null;
        this.saveFailed = false;
    }

    @Override
    public void save() {
        // Snapshot logic
        List<YieldProject> snapshot = new ArrayList<>(this.projects);
        File file = repository.getStorageFile();

        debouncer.debounce(() -> {
            boolean success = repository.save(snapshot, file);
            this.saveFailed = !success;
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public void load() {
        this.projects.clear();
        this.activeProject = null;
        this.saveFailed = false;
        List<YieldProject> loaded = repository.load();
        this.projects.addAll(loaded);
    }
}