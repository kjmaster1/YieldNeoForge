package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.client.YieldOverlay;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {

    private final Screen parent;
    private int currentX;
    private int currentY;
    private boolean isDragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private final YieldProject dummyProject;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Yield HUD Editor"));
        this.parent = parent;
        this.currentX = Config.OVERLAY_X.get();
        this.currentY = Config.OVERLAY_Y.get();
        this.dummyProject = new YieldProject("Preview Project");
        this.dummyProject.setTrackXp(true);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("yield.label.save"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        // FIX: Sanitize coordinates on open
        // If the window shrank since last time, pull the HUD back onscreen immediately
        YieldProject displayProject = ProjectManager.get().getActiveProject().orElse(dummyProject);
        int w = 150;
        int h = YieldOverlay.calculateHeight(displayProject);

        this.currentX = Mth.clamp(this.currentX, 0, this.width - w);
        this.currentY = Mth.clamp(this.currentY, 0, this.height - h);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {

        gfx.fillGradient(0, 0, this.width, this.height, 0x40000000, 0x40000000);
        gfx.drawCenteredString(this.font, "Drag the HUD to move. Press ESC to Cancel.", this.width / 2, 10, 0xFFFFFFFF);

        YieldProject displayProject = ProjectManager.get().getActiveProject().orElse(dummyProject);

        // Use the new render method that takes explicit W/H
        int w = 150;
        int h = YieldOverlay.calculateHeight(displayProject);
        YieldOverlay.renderHud(gfx, this.font, displayProject, currentX, currentY, w, h);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            YieldProject displayProject = ProjectManager.get().getActiveProject().orElse(dummyProject);
            int w = 150;
            int h = YieldOverlay.calculateHeight(displayProject);

            if (mouseX >= currentX && mouseX <= currentX + w && mouseY >= currentY && mouseY <= currentY + h) {
                isDragging = true;
                dragOffsetX = (int) mouseX - currentX;
                dragOffsetY = (int) mouseY - currentY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            YieldProject displayProject = ProjectManager.get().getActiveProject().orElse(dummyProject);
            int w = 150;
            int h = YieldOverlay.calculateHeight(displayProject);

            // FIX: Clamp dragging to screen bounds
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            this.currentX = Mth.clamp(newX, 0, this.width - w);
            this.currentY = Mth.clamp(newY, 0, this.height - h);

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        Config.OVERLAY_X.set(this.currentX);
        Config.OVERLAY_Y.set(this.currentY);
        Config.SPEC.save();
        this.minecraft.setScreen(parent);
    }

    // ... keyPressed remains the same ...
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}