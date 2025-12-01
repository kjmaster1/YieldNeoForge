package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.GoalTracker;
import com.kjmaster.yield.tracker.SessionTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YieldDashboardScreen extends Screen {

    // --- Layout Constants ---
    public static final int SIDEBAR_WIDTH = 120;
    public static final int JEI_WIDTH = 120;
    public static final int TOP_BAR_HEIGHT = 35;
    private static final int PADDING = 8;
    private static final int ITEM_HEIGHT = 24;

    // --- Colors ---
    private static final int COL_BG_TOP = 0xF0101010;
    private static final int COL_BG_BOT = 0xF0050505;
    private static final int COL_SIDEBAR = 0xFF181818;
    private static final int COL_BORDER = 0xFF303030;
    private static final int COL_GRID_ITEM_BG = 0xFF222222;
    private static final int COL_GRID_HOVER = 0xFF353535;

    // --- Widgets ---
    private ProjectList projectList;
    private EditBox nameInput;

    // Buttons
    private Button startStopButton;
    private Button deleteButton;
    private Button newProjectButton;
    private Button addGoalButton;
    private Button xpToggleButton;
    private Button moveHudButton;

    // Layouts
    private LinearLayout sidebarFooterLayout;
    private LinearLayout headerButtonsLayout;
    private final GridLayoutManager gridLayout = new GridLayoutManager();

    // --- State ---
    private ProjectGoal hoveredGoal = null;
    private boolean jeiLoaded = false;

    public YieldDashboardScreen() {
        super(Component.translatable("yield.dashboard.title"));
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        // 1. Initialize Layout Containers
        this.sidebarFooterLayout = LinearLayout.vertical().spacing(5);
        this.headerButtonsLayout = LinearLayout.horizontal().spacing(4);

        // 2. Initialize Sidebar Widgets (Bottom Up order for layout)
        this.xpToggleButton = Button.builder(Component.literal("XP"), btn -> {
            YieldProject p = getSelectedProject();
            if (p != null) {
                p.setTrackXp(!p.shouldTrackXp());
                ProjectManager.get().save();
                updateWidgetStates();
            }
        }).width(SIDEBAR_WIDTH - 10).build();

        this.moveHudButton = Button.builder(Component.translatable("yield.label.move_hud"), btn -> {
            this.minecraft.setScreen(new HudEditorScreen(this));
        }).width(SIDEBAR_WIDTH - 10).build();

        this.newProjectButton = Button.builder(Component.translatable("yield.label.new_project"), btn -> {
            ProjectManager.get().createProject("New Project");
            this.refreshList();
            List<YieldProject> projects = ProjectManager.get().getProjects();
            if (!projects.isEmpty()) {
                this.projectList.selectProject(projects.getLast());
            }
        }).width(SIDEBAR_WIDTH - 10).build();

        this.sidebarFooterLayout.addChild(this.xpToggleButton);
        this.sidebarFooterLayout.addChild(this.moveHudButton);
        this.sidebarFooterLayout.addChild(this.newProjectButton);

        this.addRenderableWidget(this.xpToggleButton);
        this.addRenderableWidget(this.moveHudButton);
        this.addRenderableWidget(this.newProjectButton);

        // 3. Initialize Header Widgets
        this.startStopButton = Button.builder(Component.translatable("yield.label.start"), btn -> toggleTracking())
                .width(80).build();

        this.addGoalButton = Button.builder(Component.translatable("yield.label.add_goal"), btn -> openItemSelector())
                .width(60).build();

        this.deleteButton = Button.builder(Component.translatable("yield.label.delete"), btn -> deleteSelected())
                .width(60).build();

        this.headerButtonsLayout.addChild(this.startStopButton);
        this.headerButtonsLayout.addChild(this.addGoalButton);
        this.headerButtonsLayout.addChild(this.deleteButton);

        this.addRenderableWidget(this.startStopButton);
        this.addRenderableWidget(this.addGoalButton);
        this.addRenderableWidget(this.deleteButton);

        // 4. Initialize Other Widgets
        this.projectList = new ProjectList(this.minecraft, SIDEBAR_WIDTH, this.height, 0, ITEM_HEIGHT);
        this.addRenderableWidget(this.projectList);

        this.nameInput = this.addRenderableWidget(new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name")));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            YieldProject p = getSelectedProject();
            if (p != null && !text.equals(p.getName())) {
                p.setName(text);
                ProjectManager.get().save();
            }
        });

        this.refreshList();
        restoreSelection();
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        int contentLeft = SIDEBAR_WIDTH + PADDING;
        int contentRight = getContentRight();

        // 1. Arrange Sidebar Footer using FrameLayout logic
        this.sidebarFooterLayout.arrangeElements();
        FrameLayout.alignInRectangle(
                this.sidebarFooterLayout,
                0,
                0,
                SIDEBAR_WIDTH,
                this.height - 5, // -5 for bottom padding
                0.5f,
                1.0f
        );

        // 2. Resize Project List
        int listBottom = this.sidebarFooterLayout.getY() - 10;
        this.projectList.updateSizeAndPosition(SIDEBAR_WIDTH, listBottom, 0);
        this.projectList.setPosition(0, 0);

        // 3. Arrange Header Buttons (Manual Position with Clamping)
        this.headerButtonsLayout.arrangeElements();
        int buttonsWidth = this.headerButtonsLayout.getWidth();

        // Calculate "Float Right" position
        int idealX = contentRight - buttonsWidth - PADDING;

        // CLAMP: Ensure we never go left of the sidebar, even if the window is tiny
        int minX = SIDEBAR_WIDTH + PADDING;
        int actualX = Math.max(minX, idealX);

        this.headerButtonsLayout.setPosition(actualX, 8); // 8 is top padding

        // 4. Position Name Input
        // It sits between the Sidebar and the Header Buttons
        // We calculate available width based on where the buttons actually ended up
        int inputAvailableWidth = actualX - contentLeft - PADDING;

        this.nameInput.setX(contentLeft);
        this.nameInput.setY(this.headerButtonsLayout.getY()); // Align top with buttons

        // Hide input if there is no room (prevent it from rendering over buttons)
        if (inputAvailableWidth < 50) {
            this.nameInput.setWidth(0);
            this.nameInput.visible = false;
        } else {
            this.nameInput.setWidth(inputAvailableWidth);
            this.nameInput.visible = true;
        }

        updateWidgetStates();

        // 5. Update Grid Layout Cache
        YieldProject p = getSelectedProject();
        if (p != null) {
            int top = TOP_BAR_HEIGHT + 15;
            int rightLimit = getContentRight() - PADDING;
            int bottomLimit = this.height - 10;

            gridLayout.update(
                    contentLeft, top,
                    rightLimit - contentLeft, bottomLimit - top,
                    18, 4,
                    p.getGoals().size()
            );
        }
    }

    private void openItemSelector() {
        if (getSelectedProject() == null) return;
        this.minecraft.setScreen(new ItemSelectionScreen(
                this,
                this::handleJeiDrop,   // Reuse existing item logic
                this::handleTagSelect  // New Tag Logic
        ));
    }

    public int getContentRight() {
        return this.width - (jeiLoaded ? JEI_WIDTH : 0);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(0, 0, SIDEBAR_WIDTH, this.height));
        areas.add(new Rect2i(0, 0, this.width, TOP_BAR_HEIGHT));
        return areas;
    }

    // For JEI Integration
    public ProjectGoal getEditingGoal() {
        return null; // Dashboard never edits directly now
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
        ProjectManager.get().save();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        // Open the editor for this new goal
        this.minecraft.setScreen(new GoalEditScreen(this, targetGoal));
    }

    private void handleTagSelect(TagKey<Item> tag) {
        YieldProject project = getSelectedProject();
        if (project == null) return;

        // 1. Resolve a display item for the tag (for the icon)
        // If the tag is empty (rare), fallback to Barrier
        var optionalItem = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(holders -> holders.stream().findFirst())
                .map(Holder::value);

        Item displayItem = optionalItem.orElse(Items.BARRIER);

        // 2. Create the Goal
        ProjectGoal goal = new ProjectGoal(
                displayItem,
                64,
                false,
                java.util.Optional.empty(),
                java.util.Optional.of(tag)
        );

        project.addGoal(goal);
        ProjectManager.get().save();

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        // Open editor immediately to let user set amount
        this.minecraft.setScreen(new GoalEditScreen(this, goal));
    }

    private void toggleTracking() {
        YieldProject p = getSelectedProject();
        if (p == null) return;
        if (SessionTracker.get().isRunning()) SessionTracker.get().stopSession();
        else {
            ProjectManager.get().setActiveProject(p);
            SessionTracker.get().startSession();
        }
        updateWidgetStates();
    }

    private void deleteSelected() {
        YieldProject p = getSelectedProject();
        if (p != null) {
            ProjectManager.get().deleteProject(p);
            refreshList();
            if (!this.projectList.children().isEmpty())
                this.projectList.setSelected(this.projectList.children().getFirst());
            updateWidgetStates();
        }
    }

    private void restoreSelection() {
        Optional<YieldProject> active = ProjectManager.get().getActiveProject();
        if (active.isPresent()) this.projectList.selectProject(active.get());
        else if (!this.projectList.children().isEmpty())
            this.projectList.setSelected(this.projectList.children().getFirst());
    }

    public void refreshList() {
        this.projectList.refresh();
    }

    private void updateWidgetStates() {
        YieldProject p = getSelectedProject();
        boolean hasSel = (p != null);
        this.xpToggleButton.visible = hasSel;
        if (hasSel) {
            String status = p.shouldTrackXp() ? "ON" : "OFF";
            int color = p.shouldTrackXp() ? 0xFF55FF55 : 0xFFAAAAAA;
            this.xpToggleButton.setMessage(Component.literal("Track XP: " + status).withColor(color));
        }
        if (this.nameInput.getWidth() > 0) {
            this.nameInput.visible = hasSel;
            this.nameInput.setEditable(hasSel);
            if (hasSel && !this.nameInput.getValue().equals(p.getName())) this.nameInput.setValue(p.getName());
        } else {
            this.nameInput.visible = false;
        }
        this.startStopButton.visible = hasSel;
        this.deleteButton.visible = hasSel;
        this.addGoalButton.visible = hasSel;
        if (hasSel && SessionTracker.get().isRunning() && ProjectManager.get().getActiveProject().isPresent()) {
            boolean isActive = ProjectManager.get().getActiveProject().get() == p;
            this.startStopButton.setMessage(isActive ? Component.translatable("yield.label.stop") : Component.translatable("yield.label.switch"));
        } else {
            this.startStopButton.setMessage(Component.translatable("yield.label.start"));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.hoveredGoal = null;

        gfx.fillGradient(0, 0, this.width, this.height, COL_BG_TOP, COL_BG_BOT);
        gfx.fill(0, 0, SIDEBAR_WIDTH, this.height, COL_SIDEBAR);
        gfx.vLine(SIDEBAR_WIDTH, 0, this.height, COL_BORDER);

        super.render(gfx, mouseX, mouseY, partialTick);

        YieldProject p = getSelectedProject();
        if (p != null) {
            renderProjectGrid(gfx, mouseX, mouseY, p);
        } else {
            int cx = SIDEBAR_WIDTH + (getContentRight() - SIDEBAR_WIDTH) / 2;
            gfx.drawCenteredString(this.font, Component.translatable("yield.label.select_prompt"), cx, this.height / 2, 0xFF888888);
        }

        if (SessionTracker.get().isRunning() && this.xpToggleButton != null && this.xpToggleButton.visible) {
            if (p != null && p.shouldTrackXp()) {
                int buttonTop = this.xpToggleButton.getY();
                int xpStatsY = buttonTop - 22;
                ItemStack xpIcon = new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE);
                gfx.renderItem(xpIcon, 8, xpStatsY);
                int xpRate = (int) SessionTracker.get().getXpPerHour();
                gfx.drawString(this.font, Component.literal(xpRate + " XP/hr"), 28, xpStatsY + 4, 0xFF55FF55, true);
            }
        }

        if (this.hoveredGoal != null) {
            renderSmartTooltip(gfx, mouseX, mouseY, this.hoveredGoal);
        }
    }

    private void renderProjectGrid(GuiGraphics gfx, int mouseX, int mouseY, YieldProject project) {
        int left = SIDEBAR_WIDTH + PADDING;
        int top = TOP_BAR_HEIGHT + 15;
        int rightLimit = getContentRight() - PADDING;
        int bottomLimit = this.height - 10;

        gfx.drawString(this.font, Component.translatable("yield.label.goals"), left, top - 12, 0xFF888888, false);

        List<ProjectGoal> goals = project.getGoals();
        if (goals.isEmpty()) {
            int areaWidth = rightLimit - left;
            Component helpText = Component.translatable("yield.label.goals_empty");
            List<FormattedCharSequence> lines = this.font.split(helpText, Math.max(50, areaWidth));
            int totalTextHeight = lines.size() * this.font.lineHeight;
            int startY = top + (bottomLimit - top - totalTextHeight) / 2;
            int centerX = left + areaWidth / 2;
            for (FormattedCharSequence line : lines) {
                gfx.drawCenteredString(this.font, line, centerX, startY, 0xFF606060);
                startY += this.font.lineHeight;
            }
            return;
        }

        gfx.enableScissor(left, top, rightLimit, bottomLimit);

        List<Rect2i> rects = gridLayout.getRects();
        int count = Math.min(goals.size(), rects.size());

        for (int i = 0; i < count; i++) {
            ProjectGoal goal = goals.get(i);
            Rect2i r = rects.get(i);
            int x = r.getX();
            int y = r.getY();
            int slotSize = r.getWidth();

            boolean isHovered = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;
            int bgColor = isHovered ? COL_GRID_HOVER : COL_GRID_ITEM_BG;
            gfx.fill(x, y, x + slotSize, y + slotSize, bgColor);

            GoalTracker tracker = SessionTracker.get().getTracker(goal);
            float progress = tracker.getProgress();

            if (progress > 0) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 200);
                int borderColor;
                if (progress >= 1.0f) borderColor = 0xFFFFD700;
                else if (progress > 0.75f) borderColor = 0xFF55FF55;
                else if (progress > 0.25f) borderColor = 0xFFFFFF55;
                else borderColor = 0xFFFF5555;
                int maxBarW = 16;
                int barWidth = (int) (maxBarW * progress);
                int barY = y + 16;
                gfx.fill(x + 1, barY, x + 1 + maxBarW, barY + 1, 0xFF000000);
                gfx.fill(x + 1, barY, x + 1 + barWidth, barY + 1, borderColor);
                gfx.pose().popPose();
            }

            gfx.renderItem(goal.getRenderStack(), x + 1, y + 1);
            gfx.renderItemDecorations(this.font, goal.getRenderStack(), x + 1, y + 1);

            if (isHovered) {
                this.hoveredGoal = goal;
            }
        }
        gfx.disableScissor();
    }

    private void renderSmartTooltip(GuiGraphics gfx, int mouseX, int mouseY, ProjectGoal goal) {
        GoalTracker tracker = SessionTracker.get().getTracker(goal);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(goal.getItem().getDescriptionId()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (goal.isStrict()) {
            tooltip.add(Component.literal("[Strict Mode]").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("yield.tooltip.progress", tracker.getCurrentCount(), goal.getTargetAmount()).withStyle(ChatFormatting.GRAY));

        int rate = (int) tracker.getItemsPerHour();
        ChatFormatting rateColor = rate > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        tooltip.add(Component.translatable("yield.tooltip.rate", rate).withStyle(rateColor));

        if (rate > 0 && tracker.getCurrentCount() < goal.getTargetAmount()) {
            int remaining = goal.getTargetAmount() - tracker.getCurrentCount();
            double hoursLeft = (double) remaining / rate;
            int minutes = (int) (hoursLeft * 60);
            String eta;
            if (minutes > 60) eta = String.format("%dh %dm", minutes / 60, minutes % 60);
            else eta = minutes + "m";
            tooltip.add(Component.translatable("yield.tooltip.eta", eta).withStyle(ChatFormatting.GRAY));
        } else if (tracker.getCurrentCount() >= goal.getTargetAmount()) {
            tooltip.add(Component.translatable("yield.tooltip.complete").withStyle(ChatFormatting.GOLD));
        }
        gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (getSelectedProject() != null && mouseX > SIDEBAR_WIDTH && mouseX < getContentRight()) {
                if (handleGridClick(mouseX, mouseY, button)) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleGridClick(double mouseX, double mouseY, int button) {
        int index = gridLayout.getIndexAt(mouseX, mouseY);
        if (index != -1) {
            YieldProject p = getSelectedProject();
            if (index < p.getGoals().size()) {
                ProjectGoal goal = p.getGoals().get(index);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                if (button == 1) { // Right Click
                    p.removeGoal(goal);
                    ProjectManager.get().save();
                    repositionElements(); // Refresh grid
                } else { // Left Click
                    this.minecraft.setScreen(new GoalEditScreen(this, goal));
                }
                return true;
            }
        }
        return false;
    }

    public YieldProject getSelectedProject() {
        ProjectEntry entry = this.projectList.getSelected();
        return entry != null ? entry.project : null;
    }

    class ProjectList extends ObjectSelectionList<ProjectEntry> {
        public ProjectList(Minecraft mc, int w, int h, int y, int itemH) {
            super(mc, w, h, y, itemH);
        }

        public void refresh() {
            this.clearEntries();
            for (YieldProject p : ProjectManager.get().getProjects()) this.addEntry(new ProjectEntry(p));
            this.setScrollAmount(this.getScrollAmount());
        }

        public void selectProject(YieldProject p) {
            for (ProjectEntry entry : this.children()) {
                if (entry.project.getId().equals(p.getId())) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderListBackground(@NotNull GuiGraphics g) {
        }
    }

    class ProjectEntry extends ObjectSelectionList.Entry<ProjectEntry> {
        final YieldProject project;

        public ProjectEntry(YieldProject p) {
            this.project = p;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(project.getName());
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int idx, int top, int left, int width, int height, int mx, int my, boolean hover, float pt) {
            boolean selected = projectList.getSelected() == this;
            if (selected) {
                gfx.fill(left, top, left + width - 4, top + height, 0xFF2A2A2A);
                gfx.fill(left, top, left + 2, top + height, 0xFF00AAFF);
            } else if (hover) gfx.fill(left, top, left + width - 4, top + height, 0xFF202020);

            Optional<YieldProject> active = ProjectManager.get().getActiveProject();
            if (active.isPresent() && active.get().getId().equals(project.getId())) {
                int indicatorX = left + 4;
                int indicatorY = top + (height - 6) / 2;
                gfx.fill(indicatorX, indicatorY, indicatorX + 4, indicatorY + 4, 0xFF55FF55);
            }

            int color = selected ? 0xFFFFFFFF : 0xFFAAAAAA;
            String name = project.getName();
            int padding = 12;
            int availableWidth = width - (padding * 2);
            if (font.width(name) > availableWidth) name = font.plainSubstrByWidth(name, availableWidth - 10) + "...";
            gfx.drawString(font, name, left + padding, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                projectList.setSelected(this);
                updateWidgetStates();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return false;
        }
    }
}