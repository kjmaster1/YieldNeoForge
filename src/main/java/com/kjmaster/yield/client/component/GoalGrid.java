package com.kjmaster.yield.client.component;

import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.event.internal.YieldEvents;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GoalGrid extends ObjectSelectionList<GoalGrid.GoalRow> {

    private final ISessionStatus sessionStatus;
    private final YieldEventBus eventBus;

    private YieldProject currentProject;
    private BiConsumer<ProjectGoal, Boolean> onGoalClicked;

    private final int slotSize = Theme.GOAL_SLOT_SIZE;
    private final int gap = Theme.GOAL_SLOT_GAP;
    private int columns = 1;

    public GoalGrid(Minecraft mc, int width, int height, int top, int bottom,
                    ISessionStatus sessionStatus, YieldEventBus eventBus) {
        super(mc, width, height, top, Theme.GOAL_SLOT_SIZE + Theme.GOAL_SLOT_GAP);
        this.sessionStatus = sessionStatus;
        this.eventBus = eventBus;
        registerEvents();
    }

    private void registerEvents() {
        // Switch context when active project changes via other controls
        eventBus.register(YieldEvents.ActiveProjectChanged.class, event -> {
            this.setProject(event.newActiveProject());
        });

        // Reflow if the current project is updated (e.g. goal added/removed, strict mode toggled)
        eventBus.register(YieldEvents.ProjectUpdated.class, event -> {
            if (currentProject != null && currentProject.id().equals(event.project().id())) {
                this.setProject(event.project());
            }
        });
    }

    public void setFixedSize(int width, int height) {
        this.updateSizeAndPosition(width, height, this.getY());
    }

    @Override
    protected boolean isValidMouseClick(int button) {
        return button == 0 || button == 1;
    }

    public void setProject(YieldProject project) {
        this.currentProject = project;
        reflow();
    }

    public void setOnGoalClicked(BiConsumer<ProjectGoal, Boolean> listener) {
        this.onGoalClicked = listener;
    }

    public void reflow() {
        this.clearEntries();
        if (currentProject == null) return;

        int availableWidth = getRowWidth();
        this.columns = Math.max(1, availableWidth / (slotSize + gap));

        List<ProjectGoal> goals = currentProject.goals();
        List<ProjectGoal> currentRowGoals = new ArrayList<>();

        for (ProjectGoal goal : goals) {
            currentRowGoals.add(goal);
            if (currentRowGoals.size() >= columns) {
                this.addEntry(new GoalRow(new ArrayList<>(currentRowGoals)));
                currentRowGoals.clear();
            }
        }

        if (!currentRowGoals.isEmpty()) {
            this.addEntry(new GoalRow(new ArrayList<>(currentRowGoals)));
        }
        this.setScrollAmount(0);
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width - 6;
    }

    @Override
    protected void renderListBackground(@NotNull GuiGraphics g) {
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (currentProject == null) {
            renderPlaceholder(gfx, "yield.label.select_prompt");
            return;
        }
        if (currentProject.goals().isEmpty()) {
            renderPlaceholder(gfx, "yield.label.goals_empty");
            return;
        }
        super.renderWidget(gfx, mouseX, mouseY, partialTick);
        GoalRow hoveredRow = getHovered();
        if (hoveredRow != null && hoveredRow.hoveredGoal != null) {
            renderSmartTooltip(gfx, mouseX, mouseY, hoveredRow.hoveredGoal);
        }
    }

    private void renderPlaceholder(GuiGraphics gfx, String key) {
        Component helpText = Component.translatable(key);
        List<FormattedCharSequence> lines = this.minecraft.font.split(helpText, Math.max(50, width - 20));
        int totalTextHeight = lines.size() * this.minecraft.font.lineHeight;
        int startY = this.getY() + (this.getHeight() - totalTextHeight) / 2;
        int centerX = this.getX() + this.getWidth() / 2;
        for (FormattedCharSequence line : lines) {
            gfx.drawCenteredString(this.minecraft.font, line, centerX, startY, 0xFF606060);
            startY += this.minecraft.font.lineHeight;
        }
    }

    private void renderSmartTooltip(GuiGraphics gfx, int mouseX, int mouseY, ProjectGoal goal) {
        GoalTracker tracker = sessionStatus.getTracker(goal);
        List<Component> tooltip = new ArrayList<>();
        if (goal.targetTag().isPresent()) {
            tooltip.add(Component.literal(goal.targetTag().get().location().toString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            tooltip.add(Component.translatable(goal.item().getDescriptionId()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        if (goal.strict()) tooltip.add(Component.literal("[Strict Mode]").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("yield.tooltip.progress", tracker.getCurrentCount(), goal.targetAmount()).withStyle(ChatFormatting.GRAY));

        int rate = (int) tracker.getItemsPerHour();
        ChatFormatting rateColor = rate > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        tooltip.add(Component.translatable("yield.tooltip.rate", rate).withStyle(rateColor));

        if (rate > 0 && tracker.getCurrentCount() < goal.targetAmount()) {
            int remaining = goal.targetAmount() - tracker.getCurrentCount();
            double hoursLeft = (double) remaining / rate;
            int minutes = (int) (hoursLeft * 60);
            String eta = (minutes > 60) ? String.format("%dh %dm", minutes / 60, minutes % 60) : minutes + "m";
            tooltip.add(Component.translatable("yield.tooltip.eta", eta).withStyle(ChatFormatting.GRAY));
        } else if (tracker.getCurrentCount() >= goal.targetAmount()) {
            tooltip.add(Component.translatable("yield.tooltip.complete").withStyle(ChatFormatting.GOLD));
        }
        gfx.renderComponentTooltip(this.minecraft.font, tooltip, mouseX, mouseY);
    }

    public class GoalRow extends ObjectSelectionList.Entry<GoalRow> {
        private final List<ProjectGoal> rowGoals;
        public ProjectGoal hoveredGoal;

        public GoalRow(List<ProjectGoal> rowGoals) {
            this.rowGoals = rowGoals;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.translatable("yield.label.goals");
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            this.hoveredGoal = null;
            for (int i = 0; i < rowGoals.size(); i++) {
                ProjectGoal goal = rowGoals.get(i);
                int x = left + (i * (slotSize + gap));
                int y = top;
                boolean slotHover = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;
                if (slotHover) this.hoveredGoal = goal;
                renderGoalSlot(gfx, goal, x, y, slotHover);
            }
        }

        private void renderGoalSlot(GuiGraphics gfx, ProjectGoal goal, int x, int y, boolean isHovered) {
            int bgColor = isHovered ? Theme.GRID_ITEM_HOVER : Theme.GRID_ITEM_BG;
            gfx.fill(x, y, x + slotSize, y + slotSize, bgColor);
            GoalTracker tracker = sessionStatus.getTracker(goal);
            float progress = tracker.getProgress();
            if (progress > 0) {
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 100);
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
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.hoveredGoal != null && onGoalClicked != null) {
                onGoalClicked.accept(this.hoveredGoal, button == 1);
                return true;
            }
            return false;
        }
    }
}