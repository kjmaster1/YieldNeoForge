package com.kjmaster.yield.client;

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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class YieldOverlay implements LayeredDraw.Layer {

    // Constants for layout
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 18; // Enough space for an icon
    private static final int TEXT_COLOR = 0xFFFFFF; // White
    private static final int RATE_COLOR = 0x55FF55; // Green
    private static final int BACKGROUND_COLOR = 0x90000000; // Semi-transparent black

    @Override
    public void render(@NotNull GuiGraphics gfx, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        // Don't render if F3 debug is open or HUD is hidden (F1)
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;

        // Check if tracking is active
        if (!SessionTracker.get().isRunning()) return;

        Optional<YieldProject> projectOpt = ProjectManager.get().getActiveProject();
        if (projectOpt.isEmpty()) return;

        YieldProject project = projectOpt.get();
        Font font = mc.font;

        // --- Layout Logic ---
        int x = 10; // Margin from left
        int y = 10; // Margin from top
        int width = 150; // Fixed width for now

        // Calculate dynamic height based on goals (limit to 5 goals to prevent screen clutter)
        int goalCount = Math.min(project.getGoals().size(), 5);
        int height = PADDING + LINE_HEIGHT + (goalCount * LINE_HEIGHT) + PADDING;

        // Draw Background Box
        gfx.fill(x, y, x + width, y + height, BACKGROUND_COLOR);

        // --- Header (Project Name) ---
        int currentY = y + PADDING;
        gfx.drawString(font, Component.literal("Project: " + project.getName()), x + PADDING, currentY + 4, TEXT_COLOR, true);

        // Draw session timer on the right
        long durationSecs = SessionTracker.get().getSessionDuration() / 1000;
        String timeStr = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60);
        int timeWidth = font.width(timeStr);
        gfx.drawString(font, Component.literal(timeStr), x + width - PADDING - timeWidth, currentY + 4, 0xAAAAAA, true);

        currentY += LINE_HEIGHT;

        // --- Goals List ---
        for (int i = 0; i < goalCount; i++) {
            ProjectGoal goal = project.getGoals().get(i);
            renderGoalRow(gfx, font, goal, x + PADDING, currentY);
            currentY += LINE_HEIGHT;
        }
    }

    private void renderGoalRow(GuiGraphics gfx, Font font, ProjectGoal goal, int x, int y) {
        ItemStack icon = goal.getRenderStack();

        // 1. Draw Item Icon
        gfx.renderItem(icon, x, y);
        // Optional: Draw decorations (damage bars, etc)
        // gfx.renderItemDecorations(font, icon, x, y);

        // 2. Draw Progress Text: "45/64"
        // Using "transient" data we added to ProjectGoal in step 1
        String progress = String.format("%d/%d", goal.getCurrentCachedAmount(), goal.getTargetAmount());
        gfx.drawString(font, Component.literal(progress), x + ICON_SIZE + 4, y + 4, TEXT_COLOR, true);

        // 3. Draw Rate Text: "120/hr"
        int rate = (int) goal.getItemsPerHour();
        if (rate > 0) {
            String rateStr = rate + "/h";
            int rateWidth = font.width(rateStr);
            // Align to the right side of the box
            gfx.drawString(font, Component.literal(rateStr), (x + 135) - rateWidth, y + 4, RATE_COLOR, true);
        }
    }
}
