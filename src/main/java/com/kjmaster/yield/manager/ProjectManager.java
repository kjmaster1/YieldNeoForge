package com.kjmaster.yield.manager;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.event.internal.YieldEvents;
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
    private final YieldEventBus eventBus;
    private final Debouncer debouncer;

    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

    // Volatile to ensure visibility across threads (Debouncer -> Render Thread)
    private volatile boolean saveFailed = false;

    public ProjectManager(ProjectRepository repository, YieldEventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.debouncer = new Debouncer();
    }

    @Override
    public void createProject(String name) {
        YieldProject project = new YieldProject(name);
        projects.add(project);
        save();
        eventBus.post(new YieldEvents.ProjectListChanged());
    }

    @Override
    public void deleteProject(YieldProject project) {
        boolean removed = projects.removeIf(p -> p.id().equals(project.id()));
        if (removed) {
            if (activeProject != null && activeProject.id().equals(project.id())) {
                setActiveProject(null);
            }
            save();
            eventBus.post(new YieldEvents.ProjectListChanged());
        }
    }

    @Override
    public void updateProject(YieldProject newProjectState) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).id().equals(newProjectState.id())) {
                projects.set(i, newProjectState);

                // If this was the active project, update the reference and notify
                if (activeProject != null && activeProject.id().equals(newProjectState.id())) {
                    this.activeProject = newProjectState;
                    eventBus.post(new YieldEvents.ActiveProjectChanged(newProjectState));
                }

                save();
                eventBus.post(new YieldEvents.ProjectUpdated(newProjectState));
                return;
            }
        }
    }

    @Override
    public void setActiveProject(YieldProject project) {
        this.activeProject = project;
        eventBus.post(new YieldEvents.ActiveProjectChanged(project));
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
        eventBus.post(new YieldEvents.ProjectListChanged());
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
        eventBus.post(new YieldEvents.ProjectListChanged());
    }
}