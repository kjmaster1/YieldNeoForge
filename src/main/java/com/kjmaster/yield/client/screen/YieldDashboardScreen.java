package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionController;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.component.DashboardHeader;
import com.kjmaster.yield.client.component.GoalGrid;
import com.kjmaster.yield.client.component.ProjectSidebar;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class YieldDashboardScreen extends Screen {

    public static final int SIDEBAR_WIDTH = Theme.SIDEBAR_WIDTH;
    public static final int TOP_BAR_HEIGHT = Theme.TOP_BAR_HEIGHT;
    private static final int PADDING = Theme.PADDING;

    private boolean jeiLoaded = false;

    private ProjectSidebar projectSidebar;
    private DashboardHeader dashboardHeader;
    private GoalGrid goalGrid;

    private final IProjectProvider projectProvider;
    private final IProjectController projectController;
    private final ISessionStatus sessionStatus;
    private final ISessionController sessionController;

    private UUID lastSelectedProjectId;

    public YieldDashboardScreen(IProjectProvider projectProvider, IProjectController projectController, ISessionStatus sessionStatus, ISessionController sessionController) {
        super(Component.translatable("yield.dashboard.title"));
        this.projectProvider = projectProvider;
        this.projectController = projectController;
        this.sessionStatus = sessionStatus;
        this.sessionController = sessionController;
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        this.projectSidebar = new ProjectSidebar(this, SIDEBAR_WIDTH, this.height, projectProvider, projectController, sessionStatus);
        this.dashboardHeader = new DashboardHeader(projectProvider, projectController, sessionStatus);
        this.goalGrid = new GoalGrid(sessionStatus);

        // NEW: Wire up the name update listener
        this.dashboardHeader.setOnProjectNameUpdated(this::updateUiState);

        setupEventListeners();

        this.addRenderableWidget(projectSidebar);
        this.addRenderableWidget(dashboardHeader);
        this.addRenderableWidget(goalGrid);

        projectSidebar.refreshList();
        repositionElements();
        restoreSelection();
    }

    private void setupEventListeners() {
        projectSidebar.setOnProjectSelected(this::onSidebarSelection);

        dashboardHeader.setCallbacks(
                this::toggleTracking,
                this::openItemSelector,
                this::deleteProject
        );

        goalGrid.setOnGoalClicked((goal, isRightClick) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            YieldProject p = projectSidebar.getSelectedProject();
            if (p == null) return;

            if (isRightClick) {
                YieldProject updated = p.removeGoal(goal);
                projectController.updateProject(updated);
                updateUiState(updated);
            } else {
                this.minecraft.setScreen(new GoalEditScreen(this, goal, p, projectController));
            }
        });
    }

    @Override
    protected void repositionElements() {
        projectSidebar.layout(0, 0, this.height);

        int contentLeft = SIDEBAR_WIDTH + PADDING;
        int headerWidth = this.width - contentLeft - PADDING;
        dashboardHeader.layout(contentLeft, PADDING, headerWidth, TOP_BAR_HEIGHT);

        int contentRight = getContentRight();
        int gridWidth = contentRight - contentLeft - PADDING;
        int gridTop = TOP_BAR_HEIGHT + 15;
        int gridHeight = this.height - gridTop - 10;
        goalGrid.layout(contentLeft, gridTop, gridWidth, gridHeight);
    }

    private void onSidebarSelection(YieldProject project) {
        if (project != null) {
            this.lastSelectedProjectId = project.id();
        }
        dashboardHeader.setProject(project);
        goalGrid.setProject(project);
    }

    public void updateUiState(YieldProject updatedProject) {
        this.lastSelectedProjectId = updatedProject.id();

        // Refresh sidebar so it re-reads names from the provider and updates the list
        projectSidebar.refreshList();
        projectSidebar.selectProject(updatedProject);

        dashboardHeader.setProject(updatedProject);
        goalGrid.setProject(updatedProject);
    }

    private void setGlobalSelection(YieldProject project) {
        projectSidebar.selectProject(project);
    }

    private void restoreSelection() {
        Optional<YieldProject> active = projectProvider.getActiveProject();

        if (active.isPresent()) {
            setGlobalSelection(active.get());
        } else {
            YieldProject toSelect = null;
            List<YieldProject> allProjects = projectProvider.getProjects();

            if (lastSelectedProjectId != null) {
                toSelect = allProjects.stream()
                        .filter(p -> p.id().equals(lastSelectedProjectId))
                        .findFirst()
                        .orElse(null);
            }

            if (toSelect == null && !allProjects.isEmpty()) {
                toSelect = allProjects.getFirst();
            }

            setGlobalSelection(toSelect);
        }
    }

    private void toggleTracking() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p == null) return;

        if (sessionStatus.isRunning()) {
            sessionController.stopSession();
        } else {
            projectController.setActiveProject(p);
            sessionController.startSession();
        }
        dashboardHeader.updateButtonStates();
    }

    private void deleteProject() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p != null) {
            projectController.deleteProject(p);
            projectSidebar.refreshList();

            if (lastSelectedProjectId != null && lastSelectedProjectId.equals(p.id())) {
                lastSelectedProjectId = null;
            }
            restoreSelection();
        }
    }

    private void openItemSelector() {
        if (projectSidebar.getSelectedProject() == null) return;
        this.minecraft.setScreen(new ItemSelectionScreen(this, this::handleJeiDrop, this::handleTagSelect));
    }

    public int getContentRight() {
        return this.width - (jeiLoaded ? Theme.JEI_WIDTH : 0);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(0, 0, SIDEBAR_WIDTH, this.height));
        areas.add(new Rect2i(0, 0, this.width, TOP_BAR_HEIGHT));
        return areas;
    }

    @Nullable
    public YieldProject getSelectedProject() {
        return projectSidebar.getSelectedProject();
    }

    @Nullable
    public ProjectGoal getEditingGoal() {
        return null;
    }

    public void handleJeiDrop(ItemStack stack) {
        YieldProject project = getSelectedProject();
        if (project == null) return;

        ProjectGoal targetGoal = null;
        for (ProjectGoal g : project.goals()) {
            if (g.item() == stack.getItem()) {
                targetGoal = g;
                break;
            }
        }

        YieldProject projectToPass = project;

        if (targetGoal == null) {
            targetGoal = ProjectGoal.fromStack(stack, 64);
            YieldProject updated = project.addGoal(targetGoal);
            projectController.updateProject(updated);
            updateUiState(updated);
            projectToPass = updated;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, targetGoal, projectToPass, projectController));
    }

    private void handleTagSelect(TagKey<Item> tag) {
        YieldProject project = getSelectedProject();
        if (project == null) return;

        var optionalItem = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(holders -> holders.stream().findFirst())
                .map(Holder::value);
        Item displayItem = optionalItem.orElse(Items.BARRIER);

        ProjectGoal goal = new ProjectGoal(displayItem, 64, false, java.util.Optional.empty(), java.util.Optional.of(tag));

        YieldProject updated = project.addGoal(goal);
        projectController.updateProject(updated);
        updateUiState(updated);

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, goal, updated, projectController));
    }

    public IProjectProvider getProjectProvider() {
        return projectProvider;
    }

    public IProjectController getProjectController() {
        return projectController;
    }

    public ISessionStatus getSessionStatus() {
        return sessionStatus;
    }
}