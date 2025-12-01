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
        this.projectSidebar = new ProjectSidebar(this, Theme.SIDEBAR_WIDTH, this.height);
        this.dashboardHeader = new DashboardHeader();
        this.goalGrid = new GoalGrid();

        // 2. Setup Event Listeners
        setupEventListeners();

        // 3. Add Components to Screen registry (for focus/event handling)
        this.addRenderableWidget(projectSidebar);
        this.addRenderableWidget(dashboardHeader);
        this.addRenderableWidget(goalGrid);

        // 4. Initial Layout & State
        repositionElements();
        restoreSelection();
    }

    private void setupEventListeners() {
        // Sidebar Events
        projectSidebar.setOnProjectSelected(this::selectProject);

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

        // 2. Calculate Content Area
        int contentLeft = Theme.SIDEBAR_WIDTH + Theme.PADDING;
        int contentRight = getContentRight();
        int contentWidth = contentRight - contentLeft - Theme.PADDING;

        // 3. Layout Header
        dashboardHeader.layout(contentLeft, Theme.PADDING, contentWidth, Theme.TOP_BAR_HEIGHT);

        // 4. Layout Grid
        int gridTop = Theme.TOP_BAR_HEIGHT + 15;
        int gridHeight = this.height - gridTop - 10;
        goalGrid.layout(contentLeft, gridTop, contentWidth, gridHeight);
    }

    private void selectProject(YieldProject project) {
        projectSidebar.selectProject(project);
        dashboardHeader.setProject(project);
        goalGrid.setProject(project);
    }

    private void restoreSelection() {
        // Try to restore active project, else select first in list
        Optional<YieldProject> active = YieldServiceRegistry.getProjectManager().getActiveProject();
        if (active.isPresent()) {
            selectProject(active.get());
        } else {
            projectSidebar.refreshList(); // Ensure list is populated
            YieldProject first = projectSidebar.getSelectedProject(); // Sidebar logic selects first by default if list populated?
            // Actually, we need to force select the first one if Sidebar doesn't auto-select
            if (first == null) {
                List<YieldProject> projects = YieldServiceRegistry.getProjectManager().getProjects();
                if (!projects.isEmpty()) {
                    selectProject(projects.getFirst());
                }
            } else {
                selectProject(first);
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

        // Refresh Header Button Text
        dashboardHeader.updateButtonStates();
        projectSidebar.refreshList(); // Updates active indicator
    }

    private void deleteProject() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p != null) {
            YieldServiceRegistry.getProjectManager().deleteProject(p);
            projectSidebar.refreshList();

            // Clear selection or select next available
            List<YieldProject> projects = YieldServiceRegistry.getProjectManager().getProjects();
            if (!projects.isEmpty()) {
                selectProject(projects.getFirst());
            } else {
                selectProject(null);
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
        areas.add(new Rect2i(0, 0, Theme.SIDEBAR_WIDTH, this.height));
        areas.add(new Rect2i(0, 0, this.width, Theme.TOP_BAR_HEIGHT));
        return areas;
    }

    @Nullable
    public YieldProject getSelectedProject() {
        return projectSidebar.getSelectedProject();
    }

    @Nullable
    public ProjectGoal getEditingGoal() {
        return null; // Dashboard never edits directly now
    }

    // --- Drop Logic (Used by JEI Plugin) ---

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

        // Open the editor for this new goal
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
        // Draw Main Backgrounds
        gfx.fillGradient(0, 0, this.width, this.height, Theme.BG_GRADIENT_TOP, Theme.BG_GRADIENT_BOT);

        // Render registered components (Sidebar, Header, Grid)
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}