package com.kjmaster.yield.client.viewmodel;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.event.internal.YieldEvents;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * ViewModel for the Dashboard Screen.
 * Manages View-Specific state (Selection) and wraps Domain Actions.
 */
public class DashboardViewModel {

    private final YieldServices services;

    // View State
    private YieldProject selectedProject;

    // View Events
    private Consumer<YieldProject> onSelectionChanged;

    public DashboardViewModel(YieldServices services) {
        this.services = services;
        initializeSelection();
        registerDomainListeners();
    }

    public void setOnSelectionChanged(Consumer<YieldProject> listener) {
        this.onSelectionChanged = listener;
    }

    private void initializeSelection() {
        Optional<YieldProject> active = services.projectProvider().getActiveProject();
        if (active.isPresent()) {
            updateSelection(active.get());
        } else if (!services.projectProvider().getProjects().isEmpty()) {
            updateSelection(services.projectProvider().getProjects().getFirst());
        } else {
            updateSelection(null);
        }
    }

    private void registerDomainListeners() {
        // If the active project changes (e.g. from hotkey), sync selection
        services.eventBus().register(YieldEvents.ActiveProjectChanged.class, event -> {
            updateSelection(event.newActiveProject());
        });

        // If projects are deleted/loaded, validate selection
        services.eventBus().register(YieldEvents.ProjectListChanged.class, event -> {
            validateSelection();
        });

        // If the currently selected project is updated (e.g. name change), refresh reference
        services.eventBus().register(YieldEvents.ProjectUpdated.class, event -> {
            if (selectedProject != null && selectedProject.id().equals(event.project().id())) {
                updateSelection(event.project());
            }
        });
    }

    private void validateSelection() {
        if (selectedProject != null) {
            boolean exists = services.projectProvider().getProjects().stream()
                    .anyMatch(p -> p.id().equals(selectedProject.id()));
            if (!exists) {
                initializeSelection(); // Revert to default
            }
        } else {
            initializeSelection();
        }
    }

    private void updateSelection(YieldProject project) {
        this.selectedProject = project;
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(project);
        }
    }

    // --- Actions ---

    public void selectProject(YieldProject project) {
        updateSelection(project);
    }

    public YieldProject getSelectedProject() {
        return selectedProject;
    }

    public void deleteSelectedProject() {
        if (selectedProject != null) {
            services.projectController().deleteProject(selectedProject);
        }
    }

    public void removeGoal(ProjectGoal goal) {
        if (selectedProject != null) {
            YieldProject updated = services.goalDomainService().removeGoal(selectedProject, goal);
            services.projectController().updateProject(updated);
        }
    }
}