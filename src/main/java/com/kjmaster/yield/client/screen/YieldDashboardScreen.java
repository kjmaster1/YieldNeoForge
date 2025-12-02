package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.component.DashboardHeader;
import com.kjmaster.yield.client.component.GoalGrid;
import com.kjmaster.yield.client.component.ProjectSidebar;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LinearLayout;
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

import static com.kjmaster.yield.client.Theme.TOP_BAR_HEIGHT;

public class YieldDashboardScreen extends Screen {

    public static final int SIDEBAR_WIDTH = Theme.SIDEBAR_WIDTH;
    private static final int PADDING = Theme.PADDING;

    private final YieldServices services;
    private boolean jeiLoaded = false;

    // Components
    private ProjectSidebar projectSidebar;
    private DashboardHeader dashboardHeader;
    private GoalGrid goalGrid;

    // Layout Root
    private final LinearLayout rootLayout = LinearLayout.horizontal();

    private UUID lastSelectedProjectId;

    public YieldDashboardScreen(YieldServices services) {
        super(Component.translatable("yield.dashboard.title"));
        this.services = services;
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        // 1. Create Widgets with Specific Dependencies (Interface Segregation)

        this.projectSidebar = new ProjectSidebar(
                this.minecraft,
                0,
                0,
                services.projectProvider(),
                services.projectController(),
                services.sessionStatus(),
                services.eventBus()
        );

        this.dashboardHeader = new DashboardHeader(services);

        this.goalGrid = new GoalGrid(
                this.minecraft,
                0,
                0,
                0,
                0,
                services.sessionStatus(),
                services.eventBus()
        );

        // Wire up logic
        wireEvents();

        // 2. Build Layout Tree
        this.rootLayout.defaultCellSetting().alignVerticallyTop();

        // Left: Sidebar
        this.rootLayout.addChild(projectSidebar, layoutSettings -> layoutSettings.padding(0));

        // Right: Content
        LinearLayout contentLayout = LinearLayout.vertical();
        contentLayout.defaultCellSetting().alignHorizontallyLeft().padding(PADDING);
        contentLayout.addChild(dashboardHeader);
        contentLayout.addChild(goalGrid);

        this.rootLayout.addChild(contentLayout);

        // 3. Register Widgets
        this.rootLayout.visitWidgets(this::addRenderableWidget);

        // 4. Calculate Positions
        repositionElements();

        // 5. Restore State
        this.projectSidebar.refreshList();
        restoreSelection();
    }

    @Override
    protected void repositionElements() {
        int rightMargin = jeiLoaded ? Theme.JEI_WIDTH : 0;
        int screenW = this.width - rightMargin;
        int screenH = this.height;

        this.projectSidebar.setFixedSize(SIDEBAR_WIDTH, screenH);

        int contentWidth = screenW - SIDEBAR_WIDTH - (PADDING * 2);
        this.dashboardHeader.setFixedSize(contentWidth, TOP_BAR_HEIGHT);

        int gridHeight = screenH - TOP_BAR_HEIGHT - (PADDING * 3);
        this.goalGrid.setFixedSize(contentWidth, gridHeight);

        this.rootLayout.arrangeElements();
        this.rootLayout.setPosition(0, 0);

        this.goalGrid.reflow();
    }

    private void wireEvents() {
        // UI Interaction Glue
        this.dashboardHeader.setOnAddGoalClicked(this::openItemSelector);
        this.projectSidebar.setOnProjectSelected(this::onSidebarSelection);

        this.goalGrid.setOnGoalClicked((goal, isRightClick) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            YieldProject p = projectSidebar.getSelectedProject();
            if (p == null) return;

            if (isRightClick) {
                // Goal Removal
                YieldProject updated = p.removeGoal(goal);
                services.projectController().updateProject(updated);
                // Note: Header/Grid update automatically via EventBus
            } else {
                // Goal Edit
                this.minecraft.setScreen(new GoalEditScreen(this, goal, p, services));
            }
        });
    }

    private void onSidebarSelection(YieldProject project) {
        if (project != null) this.lastSelectedProjectId = project.id();
        // Update components that might not be fully event-driven yet for initial selection
        dashboardHeader.setProject(project);
        goalGrid.setProject(project);
    }

    public void updateUiState(YieldProject updatedProject) {
        this.lastSelectedProjectId = updatedProject.id();
        // Sidebar refresh is usually handled by ProjectUpdated, but we might need to force selection
        projectSidebar.refreshList();
        projectSidebar.selectProject(updatedProject);

        dashboardHeader.setProject(updatedProject);
        goalGrid.setProject(updatedProject);
    }

    private void restoreSelection() {
        Optional<YieldProject> active = services.projectProvider().getActiveProject();
        if (active.isPresent()) {
            projectSidebar.selectProject(active.get());
        } else {
            List<YieldProject> allProjects = services.projectProvider().getProjects();
            YieldProject toSelect = null;
            if (lastSelectedProjectId != null) {
                toSelect = allProjects.stream().filter(p -> p.id().equals(lastSelectedProjectId)).findFirst().orElse(null);
            }
            if (toSelect == null && !allProjects.isEmpty()) {
                toSelect = allProjects.getFirst();
            }
            projectSidebar.selectProject(toSelect);
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
        areas.add(new Rect2i(0, 0, this.width, TOP_BAR_HEIGHT + (PADDING * 2)));
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
            services.projectController().updateProject(updated);
            updateUiState(updated);
            projectToPass = updated;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, targetGoal, projectToPass, services));
    }

    private void handleTagSelect(TagKey<Item> tag) {
        YieldProject project = getSelectedProject();
        if (project == null) return;
        var optionalItem = BuiltInRegistries.ITEM.getTag(tag).flatMap(holders -> holders.stream().findFirst()).map(Holder::value);
        Item displayItem = optionalItem.orElse(Items.BARRIER);
        ProjectGoal goal = new ProjectGoal(displayItem, 64, false, java.util.Optional.empty(), java.util.Optional.of(tag));
        YieldProject updated = project.addGoal(goal);
        services.projectController().updateProject(updated);
        updateUiState(updated);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, goal, updated, services));
    }
}