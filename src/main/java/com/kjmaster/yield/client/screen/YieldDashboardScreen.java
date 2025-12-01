package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.SessionTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

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
    private static final int COL_MODAL_DIM = 0xD0000000;

    // --- Widgets ---
    private ProjectList projectList;
    private EditBox nameInput;
    private Button startStopButton;
    private Button deleteButton;
    private Button newProjectButton;
    private Button addGoalButton;
    private Button xpToggleButton;
    private Button moveHudButton;

    // --- Modal Widgets ---
    private EditBox goalAmountInput;
    private Button modalSaveButton;
    private Button modalCancelButton;

    // --- State ---
    private ProjectGoal hoveredGoal = null;
    private ProjectGoal editingGoal = null;
    private boolean jeiLoaded = false;

    public YieldDashboardScreen() {
        super(Component.translatable("yield.dashboard.title"));
    }

    @Override
    protected void init() {
        this.jeiLoaded = ModList.get().isLoaded("jei");

        this.projectList = new ProjectList(this.minecraft, SIDEBAR_WIDTH, this.height, 0, ITEM_HEIGHT);
        this.addRenderableWidget(this.projectList);

        this.newProjectButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.new_project"), btn -> {
            ProjectManager.get().createProject("New Project");
            this.refreshList();
            List<YieldProject> projects = ProjectManager.get().getProjects();
            if (!projects.isEmpty()) {
                this.projectList.selectProject(projects.getLast());
            }
        }).build());

        this.nameInput = this.addRenderableWidget(new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name")));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            YieldProject p = getSelectedProject();
            if (p != null && !text.equals(p.getName())) {
                p.setName(text);
                ProjectManager.get().save();
            }
        });

        this.startStopButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.start"), btn -> toggleTracking()).build());

        this.addGoalButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.add_goal"), btn -> openItemSelector()).build());

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.delete"), btn -> deleteSelected()).build());

        this.xpToggleButton = this.addRenderableWidget(Button.builder(Component.literal("XP"), btn -> {
            YieldProject p = getSelectedProject();
            if (p != null) {
                p.setTrackXp(!p.shouldTrackXp());
                ProjectManager.get().save();
                updateWidgetStates(); // Refresh button text
            }
        }).build());

        this.moveHudButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.move_hud"), btn -> {
            this.minecraft.setScreen(new HudEditorScreen(this));
        }).build());

        this.goalAmountInput = new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.amount"));
        this.goalAmountInput.setFilter(s -> s.matches("\\d*")); // Numbers only

        this.modalSaveButton = Button.builder(Component.translatable("yield.label.save"), btn -> saveGoalEdit()).build();
        this.modalCancelButton = Button.builder(Component.translatable("yield.label.cancel"), btn -> closeGoalEditor()).build();

        closeGoalEditor();

        this.refreshList();
        restoreSelection();
        this.repositionElements();
    }

    private void openItemSelector() {
        if (getSelectedProject() == null) return;
        this.minecraft.setScreen(new ItemSelectionScreen(this, this::handleJeiDrop));
    }

    private void openGoalEditor(ProjectGoal goal) {
        this.editingGoal = goal;

        // Populate Input
        this.goalAmountInput.setValue(String.valueOf(goal.getTargetAmount()));
        this.goalAmountInput.setVisible(true);
        this.goalAmountInput.setEditable(true);

        // Buttons
        this.modalSaveButton.visible = true;
        this.modalCancelButton.visible = true;

        // Visual Disable of Background
        setMainUiActive(false);

        // FOCUS FIX: Force focus immediately
        this.setFocused(this.goalAmountInput);
        this.goalAmountInput.setFocused(true);

        repositionElements();
    }

    private void closeGoalEditor() {
        this.editingGoal = null;

        // Hide Modal Widgets
        this.goalAmountInput.setVisible(false);
        this.modalSaveButton.visible = false;
        this.modalCancelButton.visible = false;
        this.goalAmountInput.setFocused(false);

        // Re-enable Background UI
        setMainUiActive(true);
        updateWidgetStates();
    }

    private void setMainUiActive(boolean active) {
        this.nameInput.active = active;
        this.nameInput.setEditable(active);
        this.startStopButton.active = active;
        this.deleteButton.active = active;
        this.newProjectButton.active = active;
    }

    private void saveGoalEdit() {
        if (editingGoal != null) {
            try {
                int amount = Integer.parseInt(this.goalAmountInput.getValue());
                editingGoal.setTargetAmount(Math.max(1, amount));
                ProjectManager.get().save();
            } catch (NumberFormatException ignored) {
                // Ignore invalid input
            }
        }
        closeGoalEditor();
    }

    public int getContentRight() {
        return this.width - (jeiLoaded ? JEI_WIDTH : 0);
    }

    public List<Rect2i> getExclusionAreas() {
        List<Rect2i> areas = new ArrayList<>();

        // 1. Sidebar Area
        areas.add(new Rect2i(0, 0, SIDEBAR_WIDTH, this.height));

        // 2. Top Header Area (Fixes overlap with buttons)
        // This forces JEI to start rendering BELOW the header, even if buttons are pushed right.
        areas.add(new Rect2i(0, 0, this.width, TOP_BAR_HEIGHT));

        if (editingGoal != null) {
            // Block everything if modal is open
            areas.add(new Rect2i(0, 0, this.width, this.height));
            return areas;
        }

        return areas;
    }

    public ProjectGoal getEditingGoal() {
        return editingGoal;
    }

    /**
     * Called by YieldJeiPlugin when an item is dropped.
     * Adds the item and immediately opens the amount editor.
     */
    public void handleJeiDrop(net.minecraft.world.item.ItemStack stack) {
        YieldProject project = getSelectedProject();
        if (project == null) return;

        // 1. Check if this item is already a goal (Merge logic)
        ProjectGoal targetGoal = null;
        for (ProjectGoal g : project.getGoals()) {
            if (g.getItem() == stack.getItem()) {
                targetGoal = g;
                break;
            }
        }

        // 2. If new, create it.
        if (targetGoal == null) {
            // Default to 64, but the user is about to edit it anyway
            targetGoal = ProjectGoal.fromStack(stack, 64);
            project.addGoal(targetGoal);
        }

        // 3. Save state
        ProjectManager.get().save();

        // 4. PLAY SOUND
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

        // 5. OPEN EDITOR IMMEDIATELY
        openGoalEditor(targetGoal);
    }

    @Override
    protected void repositionElements() {
        int btnHeight = 20;
        int gap = 5;

        int newProjectY = this.height - 25;

        int xpToggleY = newProjectY - btnHeight - gap;

        int moveHudY = xpToggleY - btnHeight - gap;

        int statsAreaHeight = 25;
        int listBottom = moveHudY - statsAreaHeight - gap;

        this.projectList.updateSizeAndPosition(SIDEBAR_WIDTH, listBottom, 0);
        this.projectList.setPosition(0, 0);

        this.newProjectButton.setPosition(5, newProjectY);
        this.newProjectButton.setWidth(SIDEBAR_WIDTH - 10);

        if (this.xpToggleButton != null) {
            this.xpToggleButton.setPosition(5, xpToggleY);
            this.xpToggleButton.setWidth(SIDEBAR_WIDTH - 10);
        }

        this.moveHudButton.setPosition(5, moveHudY);
        this.moveHudButton.setWidth(SIDEBAR_WIDTH - 10);

        int contentLeft = SIDEBAR_WIDTH + PADDING;
        int contentRight = getContentRight() - PADDING;

        int deleteW = 60;
        int addGoalW = 60;
        int startW = 80;
        gap = 4;
        int topY = 8;

        int deleteX = contentRight - deleteW;
        int addGoalX = deleteX - gap - addGoalW;
        int startX = addGoalX - gap - startW;

        if (startX < contentLeft + 50) {
            // If squished, shift everything right and maybe overlap/hide input
            startX = contentLeft + 50 + gap;
            addGoalX = startX + startW + gap;
            deleteX = addGoalX + addGoalW + gap;
        }

        int sidebarLeft = 5;
        int bottomY = this.height - 50;

        this.xpToggleButton.setPosition(sidebarLeft, bottomY);
        this.xpToggleButton.setWidth(SIDEBAR_WIDTH - 10);

        this.deleteButton.setPosition(deleteX, topY);
        this.deleteButton.setWidth(deleteW);

        this.addGoalButton.setPosition(addGoalX, topY);
        this.addGoalButton.setWidth(addGoalW);

        this.startStopButton.setPosition(startX, topY);
        this.startStopButton.setWidth(startW);

        int inputWidth = startX - gap - contentLeft;
        this.nameInput.setPosition(contentLeft, topY);

        if (inputWidth < 40) {
            this.nameInput.visible = false;
            this.nameInput.setWidth(0);
        } else {
            this.nameInput.setWidth(inputWidth);
        }

        if (editingGoal != null) {
            int cx = this.width / 2;
            int cy = this.height / 2;

            this.goalAmountInput.setPosition(cx - 50, cy - 10);
            this.goalAmountInput.setWidth(100);

            this.modalSaveButton.setPosition(cx - 55, cy + 15);
            this.modalSaveButton.setWidth(50);

            this.modalCancelButton.setPosition(cx + 5, cy + 15);
            this.modalCancelButton.setWidth(50);
        }

        updateWidgetStates();
    }

    private void toggleTracking() {
        YieldProject p = getSelectedProject();
        if (p == null) return;
        if (SessionTracker.get().isRunning()) {
            SessionTracker.get().stopSession();
        } else {
            ProjectManager.get().setActiveProject(p);
            SessionTracker.get().startSession();
        }
        updateWidgetStates();
    }

    private void deleteSelected() {
        YieldProject p = getSelectedProject();
        if (p != null) {
            int index = -1;
            List<ProjectEntry> children = this.projectList.children();

            for (int i = 0; i < children.size(); i++) {
                if (children.get(i).project == p) {
                    index = i;
                    break;
                }
            }

            ProjectManager.get().deleteProject(p);
            refreshList();

            if (!this.projectList.children().isEmpty()) {
                int newIndex = Math.min(index, this.projectList.children().size() - 1);
                newIndex = Math.max(0, newIndex);
                this.projectList.setSelected(this.projectList.children().get(newIndex));
            }
            updateWidgetStates();
        }
    }

    private void restoreSelection() {
        Optional<YieldProject> active = ProjectManager.get().getActiveProject();
        if (active.isPresent()) {
            this.projectList.selectProject(active.get());
        } else if (!this.projectList.children().isEmpty()) {
            this.projectList.setSelected(this.projectList.children().getFirst());
        }
    }

    public void refreshList() {
        this.projectList.refresh();
    }

    private void updateWidgetStates() {
        if (editingGoal != null) return;

        YieldProject p = getSelectedProject();
        boolean hasSel = (p != null);

        this.xpToggleButton.visible = hasSel;
        if (hasSel) {
            // Update Text: "Track XP: ON" or "Track XP: OFF"
            String status = p.shouldTrackXp() ? "ON" : "OFF";
            int color = p.shouldTrackXp() ? 0xFF55FF55 : 0xFFAAAAAA;
            this.xpToggleButton.setMessage(
                    Component.literal("Track XP: " + status).withColor(color)
            );
        }

        if (this.nameInput.getWidth() > 0) {
            this.nameInput.visible = hasSel;
            this.nameInput.setEditable(hasSel);
            if (hasSel && !this.nameInput.getValue().equals(p.getName())) {
                this.nameInput.setValue(p.getName());
            }
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
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
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
            // "Select or Create a Project" -> yield.label.select_prompt
            gfx.drawCenteredString(this.font, Component.translatable("yield.label.select_prompt"), cx, this.height / 2, 0xFF888888);
        }

        if (SessionTracker.get().isRunning() && this.xpToggleButton != null && this.xpToggleButton.visible) {
            if (p != null && p.shouldTrackXp()) {
                // The button is at (height - 50), so we draw above it
                int buttonTop = this.moveHudButton.getY();
                int xpStatsY = buttonTop - 22; // 22 pixels above the button

                // Draw Icon & Text
                ItemStack xpIcon = new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE);
                gfx.renderItem(xpIcon, 8, xpStatsY);

                int xpRate = (int) SessionTracker.get().getXpPerHour();
                gfx.drawString(this.font, Component.literal(xpRate + " XP/hr"), 28, xpStatsY + 4, 0xFF55FF55, true);
            }
        }

        if (editingGoal != null) {

            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 500);

            // Dim background
            gfx.fill(0, 0, this.width, this.height, COL_MODAL_DIM);

            // Draw Modal Box
            int cx = this.width / 2;
            int cy = this.height / 2;
            int mw = 120; // Modal Width
            int mh = 80;  // Modal Height
            int mx = cx - mw / 2;
            int my = cy - mh / 2;

            // Background
            gfx.fill(mx, my, mx + mw, my + mh, 0xFF303030);
            gfx.renderOutline(mx, my, mw, mh, 0xFFFFFFFF);

            // Title text
            gfx.drawCenteredString(this.font, Component.translatable("yield.label.set_goal_amount"), cx, my + 10, 0xFFFFFFFF);

            this.goalAmountInput.render(gfx, mouseX, mouseY, partialTick);
            this.modalSaveButton.render(gfx, mouseX, mouseY, partialTick);
            this.modalCancelButton.render(gfx, mouseX, mouseY, partialTick);

            gfx.pose().popPose();

        } else if (this.hoveredGoal != null) {
            renderSmartTooltip(gfx, mouseX, mouseY, this.hoveredGoal);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
    }

    private void renderProjectGrid(GuiGraphics gfx, int mouseX, int mouseY, YieldProject project) {
        int left = SIDEBAR_WIDTH + PADDING;
        int top = TOP_BAR_HEIGHT + 15;
        int rightLimit = getContentRight() - PADDING;
        int bottomLimit = this.height - 10;

        // "Goals:" -> yield.label.goals
        gfx.drawString(this.font, Component.translatable("yield.label.goals"), left, top - 12, 0xFF888888, false);

        List<ProjectGoal> goals = project.getGoals();
        if (goals.isEmpty()) {
            int areaWidth = rightLimit - left;

            Component helpText = Component.translatable("yield.label.goals_empty");

            List<FormattedCharSequence> lines = this.font.split(helpText, Math.max(50, areaWidth)); // Ensure at least 50px to prevent infinite loops

            int totalTextHeight = lines.size() * this.font.lineHeight;
            int startY = top + (bottomLimit - top - totalTextHeight) / 2; // Center vertically in available space
            int centerX = left + areaWidth / 2;

            for (FormattedCharSequence line : lines) {
                gfx.drawCenteredString(this.font, line, centerX, startY, 0xFF606060);
                startY += this.font.lineHeight;
            }

            return;
        }

        gfx.enableScissor(left, top, rightLimit, bottomLimit);

        int slotSize = 18;
        int gap = 4;
        int availableWidth = rightLimit - left;
        int cols = Math.max(1, availableWidth / (slotSize + gap));

        int col = 0;
        int row = 0;

        for (ProjectGoal goal : goals) {
            int x = left + col * (slotSize + gap);
            int y = top + row * (slotSize + gap);

            if (x + slotSize > rightLimit) {
                col = 0;
                row++;
                x = left;
                y = top + row * (slotSize + gap);
            }

            if (y + slotSize < bottomLimit) {
                boolean isHovered = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;

                int bgColor = isHovered ? COL_GRID_HOVER : COL_GRID_ITEM_BG;
                gfx.fill(x, y, x + slotSize, y + slotSize, bgColor);

                float progress = goal.getProgress();
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

                if (isHovered && editingGoal == null) {
                    this.hoveredGoal = goal;
                }
            }

            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }

        gfx.disableScissor();
    }

    private void renderSmartTooltip(GuiGraphics gfx, int mouseX, int mouseY, ProjectGoal goal) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.translatable(goal.getItem().getDescriptionId()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        tooltip.add(Component.translatable("yield.tooltip.progress", goal.getCurrentCachedAmount(), goal.getTargetAmount()).withStyle(ChatFormatting.GRAY));

        int rate = (int) goal.getItemsPerHour();
        ChatFormatting rateColor = rate > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;

        tooltip.add(Component.translatable("yield.tooltip.rate", rate).withStyle(rateColor));

        if (rate > 0 && goal.getCurrentCachedAmount() < goal.getTargetAmount()) {
            int remaining = goal.getTargetAmount() - goal.getCurrentCachedAmount();
            double hoursLeft = (double) remaining / rate;
            int minutes = (int) (hoursLeft * 60);

            String eta;
            if (minutes > 60) eta = String.format("%dh %dm", minutes / 60, minutes % 60);
            else eta = minutes + "m";

            // "ETA: %s" -> yield.tooltip.eta
            tooltip.add(Component.translatable("yield.tooltip.eta", eta).withStyle(ChatFormatting.GRAY));
        } else if (goal.getCurrentCachedAmount() >= goal.getTargetAmount()) {
            // "Complete!" -> yield.tooltip.complete
            tooltip.add(Component.translatable("yield.tooltip.complete").withStyle(ChatFormatting.GOLD));
        }

        gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editingGoal != null) {
            if (this.goalAmountInput.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.goalAmountInput);
                return true;
            }
            if (this.modalSaveButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.modalCancelButton.mouseClicked(mouseX, mouseY, button)) return true;

            return true; // Consume all other clicks to prevent bleed-through
        }

        if (button == 0 || button == 1) {
            if (getSelectedProject() != null && mouseX > SIDEBAR_WIDTH && mouseX < getContentRight()) {
                if (handleGridClick(mouseX, mouseY, button)) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleGridClick(double mouseX, double mouseY, int button) {
        int left = SIDEBAR_WIDTH + PADDING;
        int top = TOP_BAR_HEIGHT + 15;
        int rightLimit = getContentRight() - PADDING;
        int slotSize = 18;
        int gap = 4;
        int availableWidth = rightLimit - left;
        int cols = Math.max(1, availableWidth / (slotSize + gap));

        YieldProject p = getSelectedProject();
        List<ProjectGoal> goals = p.getGoals();
        int col = 0;
        int row = 0;

        for (ProjectGoal goal : goals) {
            int x = left + col * (slotSize + gap);
            int y = top + row * (slotSize + gap);

            if (x + slotSize > rightLimit) {
                col = 0;
                row++;
                x = left;
                y = top + row * (slotSize + gap);
            }

            if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                if (button == 1) {
                    // Right Click -> Delete
                    p.removeGoal(goal);
                    ProjectManager.get().save();
                } else {
                    // Left Click -> Open Editor
                    openGoalEditor(goal);
                }
                return true;
            }
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingGoal != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeGoalEditor();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveGoalEdit();
                return true;
            }

            // Pass keys to input box
            if (this.goalAmountInput.keyPressed(keyCode, scanCode, modifiers)) return true;

            return true; // Consume other keys
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingGoal != null) {
            return this.goalAmountInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
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

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (editingGoal != null) return false;
            return super.mouseClicked(mouseX, mouseY, button);
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
            } else if (hover) {
                gfx.fill(left, top, left + width - 4, top + height, 0xFF202020);
            }

            Optional<YieldProject> active = ProjectManager.get().getActiveProject();
            if (active.isPresent() && active.get().getId().equals(project.getId())) {
                int indicatorX = left + 4;
                int indicatorY = top + (height - 6) / 2;

                // Draw a glowing green dot
                gfx.fill(indicatorX, indicatorY, indicatorX + 4, indicatorY + 4, 0xFF55FF55);
            }

            int color = selected ? 0xFFFFFFFF : 0xFFAAAAAA;
            String name = project.getName();

            int padding = 12;
            int availableWidth = width - (padding * 2);

            if (font.width(name) > availableWidth) {
                name = font.plainSubstrByWidth(name, availableWidth - 10) + "...";
            }

            gfx.drawString(font, name, left + padding, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (editingGoal != null) return false;
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