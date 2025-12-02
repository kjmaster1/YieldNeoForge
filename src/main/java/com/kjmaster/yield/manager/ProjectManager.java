package com.kjmaster.yield.manager;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.util.Debouncer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ProjectManager implements IProjectProvider, IProjectController {

    private final ProjectRepository repository;
    private final Debouncer debouncer;

    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

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
        return projects;
    }

    @Override
    public void clear() {
        this.projects.clear();
        this.activeProject = null;
    }

    @Override
    public void save() {
        List<YieldProject> snapshot = new ArrayList<>(this.projects);

        File file = repository.getStorageFile();

        debouncer.debounce(() -> {
            repository.save(snapshot, file);
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public void load() {
        this.projects.clear();
        this.activeProject = null;
        List<YieldProject> loaded = repository.load();
        this.projects.addAll(loaded);
    }
}