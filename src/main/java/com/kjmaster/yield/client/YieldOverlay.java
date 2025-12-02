package com.kjmaster.yield.client;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.GoalTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class YieldOverlay implements LayeredDraw.Layer {

    private final YieldServices services;

    public YieldOverlay(YieldServices services) {
        this.services = services;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        if (!Config.OVERLAY_ENABLED.get()) return;

        Optional<YieldProject> projectOpt = services.projectProvider().getActiveProject();
        if (projectOpt.isEmpty()) return;
        YieldProject project = projectOpt.get();
        boolean isPaused = !services.sessionStatus().isRunning();

        int width = 150;
        int height = calculateHeight(project);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        double normX = Config.OVERLAY_X.get();
        double normY = Config.OVERLAY_Y.get();
        int x = (int) (screenW * normX);
        int y = (int) (screenH * normY);

        x = Mth.clamp(x, 0, screenW - width);
        y = Mth.clamp(y, 0, screenH - height);

        renderHud(gfx, mc.font, project, x, y, width, height, isPaused, services);
    }

    public static int calculateHeight(YieldProject project) {
        int goalCount = Math.min(project.goals().size(), 5);
        return Theme.PADDING + Theme.OVERLAY_LINE_HEIGHT + (goalCount * Theme.OVERLAY_LINE_HEIGHT) + Theme.PADDING
                + (project.trackXp() ? Theme.OVERLAY_LINE_HEIGHT : 0);
    }

    public static void renderHud(GuiGraphics gfx, Font font, YieldProject project, int x, int y, int width, int height, boolean isPaused, YieldServices services) {
        int bgColor = Config.OVERLAY_COLOR.get();
        gfx.fill(x, y, x + width, y + height, bgColor);

        // --- Header Row ---
        int currentY = y + Theme.PADDING;

        // Save Failure Indicator
        if (services.projectProvider().hasSaveFailed()) {
            gfx.drawString(font, "!", x + width - 8, currentY + 4, 0xFFFF5555, true);
        }

        long durationSecs = services.sessionStatus().getSessionDuration() / 1000;
        String timeStr = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60);
        int timeWidth = font.width(timeStr);
        // Adjust time position if warning is present
        int rightMargin = services.projectProvider().hasSaveFailed() ? Theme.PADDING + 10 : Theme.PADDING;

        gfx.drawString(font, Component.literal(timeStr), x + width - rightMargin - timeWidth, currentY + 4, isPaused ? Theme.OVERLAY_TEXT_PAUSED : Theme.OVERLAY_DASH, true);

        String prefix = "Project: ";
        int gap = 5;
        int maxNameWidth = width - (Theme.PADDING * 2) - timeWidth - gap - font.width(prefix);

        String name = project.name();
        int nameColor = isPaused ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY;

        if (isPaused) name += " (Paused)";

        if (maxNameWidth > 10) {
            if (font.width(name) > maxNameWidth) {
                name = font.plainSubstrByWidth(name, maxNameWidth - font.width("...")) + "...";
            }
            gfx.drawString(font, Component.literal(prefix + name), x + Theme.PADDING, currentY + 4, nameColor, true);
        } else {
            gfx.drawString(font, Component.literal("Project"), x + Theme.PADDING, currentY + 4, nameColor, true);
        }

        currentY += Theme.OVERLAY_LINE_HEIGHT;

        if (project.trackXp()) {
            renderXpRow(gfx, font, x + Theme.PADDING, currentY, width, services);
            currentY += Theme.OVERLAY_LINE_HEIGHT;
        }

        int goalCount = Math.min(project.goals().size(), 5);
        for (int i = 0; i < goalCount; i++) {
            ProjectGoal goal = project.goals().get(i);
            renderGoalRow(gfx, font, goal, x + Theme.PADDING, currentY, width, isPaused, services);
            currentY += Theme.OVERLAY_LINE_HEIGHT;
        }
    }

    private static void renderXpRow(GuiGraphics gfx, Font font, int x, int y, int totalWidth, YieldServices services) {
        ItemStack icon = new ItemStack(Items.EXPERIENCE_BOTTLE);
        gfx.renderItem(icon, x, y);
        gfx.drawString(font, Component.literal("Experience"), x + Theme.OVERLAY_ICON_SIZE + 4, y + 4, Theme.OVERLAY_XP_LABEL, true);

        int xpRate = (int) services.sessionStatus().getXpPerHour();
        if (xpRate > 0) {
            String rateStr = xpRate + " XP/h";
            int rateWidth = font.width(rateStr);
            gfx.drawString(font, Component.literal(rateStr), (x + totalWidth - (Theme.PADDING * 2)) - rateWidth, y + 4, Theme.COLOR_XP, true);
        } else {
            gfx.drawString(font, Component.literal("-"), (x + totalWidth - (Theme.PADDING * 2)) - 5, y + 4, Theme.OVERLAY_DASH, true);
        }
    }

    private static void renderGoalRow(GuiGraphics gfx, Font font, ProjectGoal goal, int x, int y, int totalWidth, boolean isPaused, YieldServices services) {
        ItemStack icon = goal.getRenderStack();
        gfx.renderItem(icon, x, y);

        GoalTracker tracker = services.sessionStatus().getTracker(goal);
        String progress = String.format("%d/%d", tracker.getCurrentCount(), goal.targetAmount());
        int textColor = isPaused ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY;
        gfx.drawString(font, Component.literal(progress), x + Theme.OVERLAY_ICON_SIZE + 4, y + 4, textColor, true);

        double rate = tracker.getItemsPerHour();

        if (rate > 0) {
            int remaining = Math.max(0, goal.targetAmount() - tracker.getCurrentCount());
            String rightText;
            if (remaining > 0) {
                String eta = formatEta(remaining / rate);
                rightText = String.format("%.0f/h (%s)", rate, eta);
            } else {
                rightText = String.format("%.0f/h", rate);
            }

            int rightTextWidth = font.width(rightText);
            int rateColor = isPaused ? Theme.OVERLAY_RATE_PAUSED : Theme.OVERLAY_RATE;
            gfx.drawString(font, Component.literal(rightText), (x + totalWidth - (Theme.PADDING * 2)) - rightTextWidth, y + 4, rateColor, true);
        }
    }

    private static String formatEta(double hours) {
        if (Double.isInfinite(hours) || hours <= 0) return "-";
        int totalMinutes = (int) (hours * 60);
        if (totalMinutes < 60) return totalMinutes + "m";
        return String.format("%dh %dm", totalMinutes / 60, totalMinutes % 60);
    }
}