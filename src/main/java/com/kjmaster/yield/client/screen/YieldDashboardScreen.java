package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServiceRegistry;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YieldDashboardScreen extends Screen {

    // Use Theme Constants
    public static final int SIDEBAR_WIDTH = Theme.SIDEBAR_WIDTH;
    public static final int TOP_BAR_HEIGHT = Theme.TOP_BAR_HEIGHT;
    private static final int PADDING = Theme.PADDING;

    private boolean jeiLoaded = false;

    // --- Components ---
    private ProjectSidebar projectSidebar;
    private DashboardHeader dashboardHeader;
    private GoalGrid goalGrid;

    public YieldDashboardScreen() {
        super(Component.translatable("yield.dashboard.title"));
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        // 1. Initialize Components
        this.projectSidebar = new ProjectSidebar(this, SIDEBAR_WIDTH, this.height);
        this.dashboardHeader = new DashboardHeader();
        this.goalGrid = new GoalGrid();

        // 2. Setup Event Listeners
        setupEventListeners();

        // 3. Add Components to Screen registry (for focus/event handling)
        this.addRenderableWidget(projectSidebar);
        this.addRenderableWidget(dashboardHeader);
        this.addRenderableWidget(goalGrid);

        // 4. Populate List
        projectSidebar.refreshList();

        // 5. Initial Layout & State
        repositionElements();
        restoreSelection();
    }

    private void setupEventListeners() {
        // Sidebar Events
        projectSidebar.setOnProjectSelected(this::onSidebarSelection);

        // Header Events
        dashboardHeader.setCallbacks(
                this::toggleTracking,    // Start/Stop
                this::openItemSelector,  // Add Goal
                this::deleteProject      // Delete Project
        );

        // Grid Events
        goalGrid.setOnGoalClicked((goal, isRightClick) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            YieldProject p = projectSidebar.getSelectedProject();
            if (p == null) return;

            if (isRightClick) {
                p.removeGoal(goal);
                YieldServiceRegistry.getProjectManager().save();
                goalGrid.recalculateLayout();
            } else {
                this.minecraft.setScreen(new GoalEditScreen(this, goal));
            }
        });
    }

    @Override
    protected void repositionElements() {
        // 1. Layout Sidebar
        projectSidebar.layout(0, 0, this.height);

        // 2. Common Coordinates
        int contentLeft = SIDEBAR_WIDTH + PADDING;

        // 3. Layout Header
        int headerWidth = this.width - contentLeft - PADDING;
        dashboardHeader.layout(contentLeft, PADDING, headerWidth, TOP_BAR_HEIGHT);

        // 4. Layout Grid
        int contentRight = getContentRight();
        int gridWidth = contentRight - contentLeft - PADDING;

        int gridTop = TOP_BAR_HEIGHT + 15;
        int gridHeight = this.height - gridTop - 10;
        goalGrid.layout(contentLeft, gridTop, gridWidth, gridHeight);
    }

    private void onSidebarSelection(YieldProject project) {
        dashboardHeader.setProject(project);
        goalGrid.setProject(project);
    }

    private void setGlobalSelection(YieldProject project) {
        projectSidebar.selectProject(project);
    }

    private void restoreSelection() {
        Optional<YieldProject> active = YieldServiceRegistry.getProjectManager().getActiveProject();
        if (active.isPresent()) {
            setGlobalSelection(active.get());
        } else {
            YieldProject first = projectSidebar.getSelectedProject();
            if (first == null) {
                List<YieldProject> projects = YieldServiceRegistry.getProjectManager().getProjects();
                if (!projects.isEmpty()) {
                    setGlobalSelection(projects.getFirst());
                }
            } else {
                setGlobalSelection(first);
            }
        }
    }

    // --- Actions ---

    private void toggleTracking() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p == null) return;

        if (YieldServiceRegistry.getSessionTracker().isRunning()) {
            YieldServiceRegistry.getSessionTracker().stopSession();
        } else {
            YieldServiceRegistry.getProjectManager().setActiveProject(p);
            YieldServiceRegistry.getSessionTracker().startSession();
        }

        dashboardHeader.updateButtonStates();
    }

    private void deleteProject() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p != null) {
            YieldServiceRegistry.getProjectManager().deleteProject(p);
            projectSidebar.refreshList();

            // Clear selection or select next available
            List<YieldProject> projects = YieldServiceRegistry.getProjectManager().getProjects();
            if (!projects.isEmpty()) {
                setGlobalSelection(projects.getFirst());
            } else {
                setGlobalSelection(null);
            }
        }
    }

    private void openItemSelector() {
        if (projectSidebar.getSelectedProject() == null) return;
        this.minecraft.setScreen(new ItemSelectionScreen(
                this,
                this::handleJeiDrop,
                this::handleTagSelect
        ));
    }

    // --- External / JEI Helpers ---

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
        for (ProjectGoal g : project.getGoals()) {
            if (g.getItem() == stack.getItem()) {
                targetGoal = g;
                break;
            }
        }
        if (targetGoal == null) {
            targetGoal = ProjectGoal.fromStack(stack, 64);
            project.addGoal(targetGoal);
        }
        YieldServiceRegistry.getProjectManager().save();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        this.minecraft.setScreen(new GoalEditScreen(this, targetGoal));
    }

    private void handleTagSelect(TagKey<Item> tag) {
        YieldProject project = getSelectedProject();
        if (project == null) return;

        var optionalItem = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(holders -> holders.stream().findFirst())
                .map(Holder::value);
        Item displayItem = optionalItem.orElse(Items.BARRIER);

        ProjectGoal goal = new ProjectGoal(
                displayItem,
                64,
                false,
                java.util.Optional.empty(),
                java.util.Optional.of(tag)
        );

        project.addGoal(goal);
        YieldServiceRegistry.getProjectManager().save();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, goal));
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, Theme.BG_GRADIENT_TOP, Theme.BG_GRADIENT_BOT);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}