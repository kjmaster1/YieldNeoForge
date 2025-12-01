package com.kjmaster.yield.client;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.SessionTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth; // Import for clamping
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class YieldOverlay implements LayeredDraw.Layer {

    // Constants for layout
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 18;

    // Colors
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_COLOR_PAUSED = 0xFFAAAAAA;
    private static final int RATE_COLOR = 0x55FF55;
    private static final int RATE_COLOR_PAUSED = 0xFF888888;
    private static final int XP_COLOR = 0xAAFFAA;

    @Override
    public void render(@NotNull GuiGraphics gfx, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        if (!Config.OVERLAY_ENABLED.get()) return;


        Optional<YieldProject> projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isEmpty()) return;
        YieldProject project = projectOpt.get();
        boolean isPaused = !SessionTracker.get().isRunning();

        // 1. Calculate Size
        int width = 150;
        int height = calculateHeight(project);

        // 2. Get Raw Config Coordinates
        int rawX = Config.OVERLAY_X.get();
        int rawY = Config.OVERLAY_Y.get();

        // 3. CLAMP coordinates to Screen Bounds
        // This ensures if the window shrinks, the HUD slides back into view
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = Mth.clamp(rawX, 0, screenW - width);
        int y = Mth.clamp(rawY, 0, screenH - height);

        // 4. Render
        renderHud(gfx, mc.font, project, x, y, width, height, isPaused);
    }

    public static int calculateHeight(YieldProject project) {
        int goalCount = Math.min(project.getGoals().size(), 5);
        int height = PADDING + LINE_HEIGHT + (goalCount * LINE_HEIGHT) + PADDING;
        if (project.shouldTrackXp()) {
            height += LINE_HEIGHT;
        }
        return height;
    }

    /**
     * Updated static helper that accepts pre-calculated width/height
     */
    public static void renderHud(GuiGraphics gfx, Font font, YieldProject project, int x, int y, int width, int height, boolean isPaused) {
        // Draw Background
        int bgColor = Config.OVERLAY_COLOR.get();
        gfx.fill(x, y, x + width, y + height, bgColor);

        // --- Header Row ---
        int currentY = y + PADDING;

        long durationSecs = SessionTracker.get().getSessionDuration() / 1000;
        String timeStr = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60);
        int timeWidth = font.width(timeStr);
        gfx.drawString(font, Component.literal(timeStr), x + width - PADDING - timeWidth, currentY + 4, isPaused ? 0xFF666666 : 0xAAAAAA, true);

        String prefix = "Project: ";
        int gap = 5;
        int maxNameWidth = width - (PADDING * 2) - timeWidth - gap - font.width(prefix);

        String name = project.getName();
        int nameColor = isPaused ? TEXT_COLOR_PAUSED : TEXT_COLOR;

        if (isPaused) {
            // Append status
            name += " (Paused)";
        }

        if (maxNameWidth > 10) {
            if (font.width(name) > maxNameWidth) {
                name = font.plainSubstrByWidth(name, maxNameWidth - font.width("...")) + "...";
            }
            gfx.drawString(font, Component.literal(prefix + name), x + PADDING, currentY + 4, nameColor, true);
        } else {
            gfx.drawString(font, Component.literal("Project"), x + PADDING, currentY + 4, nameColor, true);
        }

        currentY += LINE_HEIGHT;

        // --- XP Row ---
        if (project.shouldTrackXp()) {
            renderXpRow(gfx, font, x + PADDING, currentY, width);
            currentY += LINE_HEIGHT;
        }

        // --- Goals ---
        int goalCount = Math.min(project.getGoals().size(), 5);
        for (int i = 0; i < goalCount; i++) {
            ProjectGoal goal = project.getGoals().get(i);
            renderGoalRow(gfx, font, goal, x + PADDING, currentY, width, isPaused);
            currentY += LINE_HEIGHT;
        }
    }

    // ... renderXpRow and renderGoalRow remain unchanged ...
    private static void renderXpRow(GuiGraphics gfx, Font font, int x, int y, int totalWidth) {
        ItemStack icon = new ItemStack(Items.EXPERIENCE_BOTTLE);
        gfx.renderItem(icon, x, y);
        gfx.drawString(font, Component.literal("Experience"), x + ICON_SIZE + 4, y + 4, XP_COLOR, true);

        int xpRate = (int) SessionTracker.get().getXpPerHour();
        if (xpRate > 0) {
            String rateStr = xpRate + " XP/h";
            int rateWidth = font.width(rateStr);
            gfx.drawString(font, Component.literal(rateStr), (x + totalWidth - (PADDING * 2)) - rateWidth, y + 4, RATE_COLOR, true);
        } else {
            gfx.drawString(font, Component.literal("-"), (x + totalWidth - (PADDING * 2)) - 5, y + 4, 0xAAAAAA, true);
        }
    }

    private static void renderGoalRow(GuiGraphics gfx, Font font, ProjectGoal goal, int x, int y, int totalWidth, boolean isPaused) {
        ItemStack icon = goal.getRenderStack();
        gfx.renderItem(icon, x, y);

        String progress = String.format("%d/%d", goal.getCurrentCachedAmount(), goal.getTargetAmount());

        int textColor = isPaused ? TEXT_COLOR_PAUSED : TEXT_COLOR;
        gfx.drawString(font, Component.literal(progress), x + ICON_SIZE + 4, y + 4, textColor, true);

        int rate = (int) goal.getItemsPerHour();
        int rateColor = isPaused ? RATE_COLOR_PAUSED : RATE_COLOR;

        if (rate > 0) {
            String rateStr = rate + "/h";
            int rateWidth = font.width(rateStr);
            gfx.drawString(font, Component.literal(rateStr), (x + totalWidth - (PADDING * 2)) - rateWidth, y + 4, rateColor, true);
        }
    }

    public static void renderHud(GuiGraphics gfx, Font font, YieldProject project, int x, int y, int width, int height) {
        renderHud(gfx, font, project, x, y, width, height, false);
    }
}