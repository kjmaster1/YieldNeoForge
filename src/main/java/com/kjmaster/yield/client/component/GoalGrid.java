package com.kjmaster.yield.client.component;

import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GoalGrid implements Renderable, GuiEventListener, NarratableEntry {

    private final Minecraft minecraft;
    private final Font font;
    private final ISessionStatus sessionStatus;

    // Use Native GridLayout to manage positions
    private final GridLayout layoutGrid;

    private YieldProject currentProject;
    private ProjectGoal hoveredGoal;
    private int x, y, width, height;
    private BiConsumer<ProjectGoal, Boolean> onGoalClicked;

    public GoalGrid(ISessionStatus session) {
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.sessionStatus = session;
        this.layoutGrid = new GridLayout();
        this.layoutGrid.spacing(Theme.GOAL_SLOT_GAP);
    }

    public void setProject(YieldProject project) {
        this.currentProject = project;
        recalculateLayout();
    }

    public void setOnGoalClicked(BiConsumer<ProjectGoal, Boolean> listener) {
        this.onGoalClicked = listener;
    }

    public void layout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        recalculateLayout();
    }

    public void recalculateLayout() {
        if (currentProject == null) return;

        // Clear logic would go here if we were holding widgets
        // Recalculate grid dimensions
        // With native GridLayout, we typically add widgets.
        // Since we are rendering manually, we use it to calculate X/Y.

        // Reset grid
        this.layoutGrid.setX(x);
        this.layoutGrid.setY(y);
        // We don't actually add children because we want to render manually for now
        // to keep the "lightweight" item rendering vs full widget overhead.
        // However, we can simulate the math:
        // Col count = width / (size + gap)
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.hoveredGoal = null;
        if (currentProject == null) {
            renderPlaceholder(gfx, "yield.label.select_prompt");
            return;
        }
        List<ProjectGoal> goals = currentProject.goals();
        if (goals.isEmpty()) {
            renderPlaceholder(gfx, "yield.label.goals_empty");
            return;
        }

        gfx.drawString(this.font, Component.translatable("yield.label.goals"), x, y - 12, Theme.TEXT_PRIMARY, false);
        gfx.enableScissor(x, y, x + width, y + height);

        // Manual Grid Logic using Theme constants (Simple and efficient)
        int slotSize = Theme.GOAL_SLOT_SIZE;
        int gap = Theme.GOAL_SLOT_GAP;
        int cols = Math.max(1, width / (slotSize + gap));

        for (int i = 0; i < goals.size(); i++) {
            ProjectGoal goal = goals.get(i);

            int col = i % cols;
            int row = i / cols;

            int drawX = x + col * (slotSize + gap);
            int drawY = y + row * (slotSize + gap);

            // Check visibility (Vertical Scroll clipping)
            if (drawY + slotSize > y + height) break;

            renderGoalSlot(gfx, goal, drawX, drawY, slotSize, mouseX, mouseY);
        }

        gfx.disableScissor();

        if (this.hoveredGoal != null) {
            renderSmartTooltip(gfx, mouseX, mouseY, this.hoveredGoal);
        }
    }

    private void renderGoalSlot(GuiGraphics gfx, ProjectGoal goal, int rx, int ry, int size, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= rx && mouseX < rx + size && mouseY >= ry && mouseY < ry + size;
        int bgColor = isHovered ? Theme.GRID_ITEM_HOVER : Theme.GRID_ITEM_BG;
        gfx.fill(rx, ry, rx + size, ry + size, bgColor);

        GoalTracker tracker = sessionStatus.getTracker(goal);
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
            int barY = ry + 16;
            gfx.fill(rx + 1, barY, rx + 1 + maxBarW, barY + 1, 0xFF000000);
            gfx.fill(rx + 1, barY, rx + 1 + barWidth, barY + 1, borderColor);
            gfx.pose().popPose();
        }

        gfx.renderItem(goal.getRenderStack(), rx + 1, ry + 1);
        if (isHovered) this.hoveredGoal = goal;
    }

    private void renderPlaceholder(GuiGraphics gfx, String key) {
        Component helpText = Component.translatable(key);
        List<FormattedCharSequence> lines = this.font.split(helpText, Math.max(50, width));
        int totalTextHeight = lines.size() * this.font.lineHeight;
        int startY = y + (height - totalTextHeight) / 2;
        int centerX = x + width / 2;
        for (FormattedCharSequence line : lines) {
            gfx.drawCenteredString(this.font, line, centerX, startY, 0xFF606060);
            startY += this.font.lineHeight;
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
        gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentProject == null) return false;

        int slotSize = Theme.GOAL_SLOT_SIZE;
        int gap = Theme.GOAL_SLOT_GAP;
        int cols = Math.max(1, width / (slotSize + gap));

        // Manual Hit Test (matching render logic)
        for (int i = 0; i < currentProject.goals().size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int drawX = x + col * (slotSize + gap);
            int drawY = y + row * (slotSize + gap);

            if (mouseX >= drawX && mouseX < drawX + slotSize && mouseY >= drawY && mouseY < drawY + slotSize) {
                ProjectGoal goal = currentProject.goals().get(i);
                if (onGoalClicked != null) {
                    onGoalClicked.accept(goal, button == 1);
                    return true;
                }
            }
        }
        return false;
    }

    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
}