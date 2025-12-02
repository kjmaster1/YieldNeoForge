package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.component.DashboardHeader;
import com.kjmaster.yield.client.component.GoalGrid;
import com.kjmaster.yield.client.component.ProjectSidebar;
import com.kjmaster.yield.client.viewmodel.DashboardViewModel;
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

import static com.kjmaster.yield.client.Theme.PADDING;
import static com.kjmaster.yield.client.Theme.TOP_BAR_HEIGHT;

public class YieldDashboardScreen extends Screen {

    // Dependencies
    private final YieldServices services;
    private final DashboardViewModel viewModel;

    // State
    private boolean jeiLoaded = false;

    // Components
    private ProjectSidebar projectSidebar;
    private DashboardHeader dashboardHeader;
    private GoalGrid goalGrid;

    // Layout
    private final LinearLayout rootLayout = LinearLayout.horizontal();

    public YieldDashboardScreen(YieldServices services) {
        super(Component.translatable("yield.dashboard.title"));
        this.services = services;
        this.viewModel = new DashboardViewModel(services);
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        // 1. Instantiate Components
        this.projectSidebar = new ProjectSidebar(
                this.minecraft, 0, 0,
                services.projectProvider(),
                services.projectController(),
                services.sessionStatus(),
                services.eventBus()
        );

        this.dashboardHeader = new DashboardHeader(services);

        this.goalGrid = new GoalGrid(
                this.minecraft, 0, 0, 0, 0,
                services.sessionStatus(),
                services.eventBus()
        );

        // 2. Wire ViewModel & Actions
        wireViewModel();

        // 3. Build Layout Hierarchy
        // We use a root horizontal layout: [Sidebar] [Vertical Content]
        this.rootLayout.defaultCellSetting().alignVerticallyTop().padding(0);
        this.rootLayout.addChild(projectSidebar);

        LinearLayout contentLayout = LinearLayout.vertical();
        contentLayout.defaultCellSetting().alignHorizontallyLeft().padding(PADDING);
        contentLayout.addChild(dashboardHeader);
        contentLayout.addChild(goalGrid);

        this.rootLayout.addChild(contentLayout);

        // 4. Register to Screen
        this.rootLayout.visitWidgets(this::addRenderableWidget);

        // 5. Layout & Position
        repositionElements();
    }

    private void wireViewModel() {
        // View -> ViewModel
        this.projectSidebar.setOnProjectSelected(viewModel::selectProject);

        // ViewModel -> View
        this.viewModel.setOnSelectionChanged(this::updateUiSelection);

        // Actions
        this.dashboardHeader.setOnAddGoalClicked(this::openItemSelector);

        this.goalGrid.setOnGoalClicked((goal, isRightClick) -> {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (isRightClick) {
                viewModel.removeGoal(goal);
            } else {
                YieldProject p = viewModel.getSelectedProject();
                if (p != null) {
                    this.minecraft.setScreen(new GoalEditScreen(this, goal, p, services));
                }
            }
        });
    }

    private void updateUiSelection(YieldProject project) {
        // Synchronize all components to the ViewModel state
        this.projectSidebar.selectProject(project);
        this.dashboardHeader.setProject(project);
        this.goalGrid.setProject(project);
    }

    /**
     * Declarative-style Layout Calculation.
     * Calculates component sizes based on screen constraints and applies them.
     */
    @Override
    protected void repositionElements() {
        // Constants
        final int sidebarWidth = Theme.SIDEBAR_WIDTH;
        final int headerHeight = TOP_BAR_HEIGHT;
        final int padding = PADDING;
        final int jeiWidth = jeiLoaded ? Theme.JEI_WIDTH : 0;

        // Available Dimensions
        final int totalWidth = this.width - jeiWidth;
        final int contentWidth = totalWidth - sidebarWidth - (padding * 2);
        final int contentHeight = this.height - headerHeight - (padding * 3);

        // Apply Constraints
        this.projectSidebar.setFixedSize(sidebarWidth, this.height);
        this.dashboardHeader.setFixedSize(contentWidth, headerHeight);
        this.goalGrid.setFixedSize(contentWidth, contentHeight);

        // Apply Layout Engine
        this.rootLayout.arrangeElements();
        this.rootLayout.setPosition(0, 0);

        // Refresh Grid internals
        this.goalGrid.reflow();
    }

    // --- Actions & Helpers ---

    private void openItemSelector() {
        if (viewModel.getSelectedProject() == null) return;
        this.minecraft.setScreen(new ItemSelectionScreen(this, this::handleJeiDrop, this::handleTagSelect));
    }

    public void updateUiState(YieldProject updatedProject) {
        // Called by Edit Screen to force refresh if needed, though EventBus usually handles this.
        // We defer to ViewModel to keep state consistent.
        viewModel.selectProject(updatedProject);
    }

    public void handleJeiDrop(ItemStack stack) {
        YieldProject project = viewModel.getSelectedProject();
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
            YieldProject updated = services.goalDomainService().addGoal(project, targetGoal);
            services.projectController().updateProject(updated);
            projectToPass = updated;
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, targetGoal, projectToPass, services));
    }

    private void handleTagSelect(TagKey<Item> tag) {
        YieldProject project = viewModel.getSelectedProject();
        if (project == null) return;

        var optionalItem = BuiltInRegistries.ITEM.getTag(tag).flatMap(holders -> holders.stream().findFirst()).map(Holder::value);
        Item displayItem = optionalItem.orElse(Items.BARRIER);

        ProjectGoal goal = new ProjectGoal(displayItem, 64, false, java.util.Optional.empty(), java.util.Optional.of(tag));
        YieldProject updated = services.goalDomainService().addGoal(project, goal);
        services.projectController().updateProject(updated);

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.minecraft.setScreen(new GoalEditScreen(this, goal, updated, services));
    }

    @Nullable
    public YieldProject getSelectedProject() {
        return viewModel.getSelectedProject();
    }

    @Nullable
    public ProjectGoal getEditingGoal() {
        return null;
    }

    public int getContentRight() {
        return this.width - (jeiLoaded ? Theme.JEI_WIDTH : 0);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(0, 0, Theme.SIDEBAR_WIDTH, this.height));
        areas.add(new Rect2i(0, 0, this.width, TOP_BAR_HEIGHT + (PADDING * 2)));
        return areas;
    }
}