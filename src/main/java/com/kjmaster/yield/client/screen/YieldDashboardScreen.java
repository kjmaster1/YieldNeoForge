package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.component.DashboardHeader;
import com.kjmaster.yield.client.component.GoalGrid;
import com.kjmaster.yield.client.component.ProjectSidebar;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

        // 1. Create Widgets (Dimensions will be set by layout/repositionElements)
        this.projectSidebar = new ProjectSidebar(this.minecraft, 0, 0, services);
        this.dashboardHeader = new DashboardHeader(services);
        this.goalGrid = new GoalGrid(this.minecraft, 0, 0, 0, 0, services);

        // Wire up logic
        wireEvents();

        // 2. Build Layout Tree
        // Root: Horizontal [Sidebar | Content]
        this.rootLayout.defaultCellSetting().alignVerticallyTop();

        // Left: Sidebar (Fixed Width, Full Height)
        this.rootLayout.addChild(projectSidebar, layoutSettings ->
                layoutSettings.padding(0)
        );

        // Right: Content Vertical [Header | Grid]
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
        // Calculate available area
        int rightMargin = jeiLoaded ? Theme.JEI_WIDTH : 0;
        int screenW = this.width - rightMargin;
        int screenH = this.height;

        // 1. Configure Sidebar fixed size
        this.projectSidebar.setFixedSize(SIDEBAR_WIDTH, screenH);

        // 2. Configure Header fixed size (Fill remaining width)
        // Subtract sidebar width and padding (Left padding for content + Right padding)
        int contentWidth = screenW - SIDEBAR_WIDTH - (PADDING * 2);
        this.dashboardHeader.setFixedSize(contentWidth, TOP_BAR_HEIGHT);

        // 3. Configure Grid fixed size (Fill remaining height)
        // H = Total - Header - Padding*3 (Header Top Gap, Header/Grid Gap, Bottom Gap)
        int gridHeight = screenH - TOP_BAR_HEIGHT - (PADDING * 3);
        this.goalGrid.setFixedSize(contentWidth, gridHeight);

        // 4. Apply Layout
        // This forces the LinearLayout to set X, Y on all children based on their sizes and structure
        this.rootLayout.arrangeElements();
        this.rootLayout.setPosition(0, 0); // Top-Left anchor

        // 5. Refresh Grid internals now that size is known
        this.goalGrid.reflow();
    }

    private void wireEvents() {
        this.dashboardHeader.setOnProjectNameUpdated(this::updateUiState);
        this.projectSidebar.setOnProjectSelected(this::onSidebarSelection);

        this.dashboardHeader.setCallbacks(
                this::toggleTracking,
                this::openItemSelector,
                this::deleteProject
        );

        this.goalGrid.setOnGoalClicked((goal, isRightClick) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            YieldProject p = projectSidebar.getSelectedProject();
            if (p == null) return;

            if (isRightClick) {
                YieldProject updated = p.removeGoal(goal);
                services.projectController().updateProject(updated);
                updateUiState(updated);
            } else {
                this.minecraft.setScreen(new GoalEditScreen(this, goal, p, services));
            }
        });
    }

    private void onSidebarSelection(YieldProject project) {
        if (project != null) this.lastSelectedProjectId = project.id();
        dashboardHeader.setProject(project);
        goalGrid.setProject(project);
    }

    public void updateUiState(YieldProject updatedProject) {
        this.lastSelectedProjectId = updatedProject.id();
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

    private void toggleTracking() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p == null) return;
        if (services.sessionStatus().isRunning()) {
            services.sessionController().stopSession();
        } else {
            services.projectController().setActiveProject(p);
            services.sessionController().startSession();
        }
        dashboardHeader.updateButtonStates();
    }

    private void deleteProject() {
        YieldProject p = projectSidebar.getSelectedProject();
        if (p != null) {
            services.projectController().deleteProject(p);
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