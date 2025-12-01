package com.kjmaster.yield.client.component;

import com.kjmaster.yield.YieldServiceRegistry;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.screen.GridLayoutManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class GoalGrid implements Renderable, GuiEventListener, NarratableEntry {

    private final Minecraft minecraft;
    private final Font font;
    private final GridLayoutManager gridLayout = new GridLayoutManager();

    private YieldProject currentProject;
    private ProjectGoal hoveredGoal;

    // Bounds
    private int x, y, width, height;

    // Callbacks
    private BiConsumer<ProjectGoal, Boolean> onGoalClicked; // Goal, isRightClick

    public GoalGrid() {
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
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
        if (currentProject != null) {
            gridLayout.update(
                    x, y,
                    width, height,
                    18, 4, // Slot size 18, gap 4
                    currentProject.getGoals().size()
            );
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.hoveredGoal = null;

        if (currentProject == null) {
            renderPlaceholder(gfx, "yield.label.select_prompt");
            return;
        }

        List<ProjectGoal> goals = currentProject.getGoals();
        if (goals.isEmpty()) {
            renderPlaceholder(gfx, "yield.label.goals_empty");
            return;
        }

        // Draw Label
        gfx.drawString(this.font, Component.translatable("yield.label.goals"), x, y - 12, Theme.TEXT_HEADER, false);

        // Scissor for scrolling/overflow protection
        gfx.enableScissor(x, y, x + width, y + height);

        List<Rect2i> rects = gridLayout.getRects();
        int count = Math.min(goals.size(), rects.size());

        for (int i = 0; i < count; i++) {
            ProjectGoal goal = goals.get(i);
            Rect2i r = rects.get(i);
            renderGoalSlot(gfx, goal, r, mouseX, mouseY);
        }

        gfx.disableScissor();

        if (this.hoveredGoal != null) {
            renderSmartTooltip(gfx, mouseX, mouseY, this.hoveredGoal);
        }
    }

    private void renderGoalSlot(GuiGraphics gfx, ProjectGoal goal, Rect2i r, int mouseX, int mouseY) {
        int rx = r.getX();
        int ry = r.getY();
        int size = r.getWidth();

        boolean isHovered = mouseX >= rx && mouseX < rx + size && mouseY >= ry && mouseY < ry + size;

        // Background
        int bgColor = isHovered ? Theme.GRID_ITEM_HOVER : Theme.GRID_ITEM_BG;
        gfx.fill(rx, ry, rx + size, ry + size, bgColor);

        // Progress Bar
        GoalTracker tracker = YieldServiceRegistry.getSessionTracker().getTracker(goal);
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

        // Item Icon
        gfx.renderItem(goal.getRenderStack(), rx + 1, ry + 1);
        gfx.renderItemDecorations(this.font, goal.getRenderStack(), rx + 1, ry + 1);

        if (isHovered) {
            this.hoveredGoal = goal;
        }
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
        GoalTracker tracker = YieldServiceRegistry.getSessionTracker().getTracker(goal);
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
        if (currentProject == null) return false;

        // Check grid clicks
        int index = gridLayout.getIndexAt(mouseX, mouseY);
        if (index != -1 && index < currentProject.getGoals().size()) {
            ProjectGoal goal = currentProject.getGoals().get(index);
            if (onGoalClicked != null) {
                onGoalClicked.accept(goal, button == 1); // 1 = Right Click
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}
    @Override
    public boolean isFocused() { return false; }
    @Override
    public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
}