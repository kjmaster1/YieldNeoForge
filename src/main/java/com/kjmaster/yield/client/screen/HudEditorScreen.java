package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.YieldOverlay;
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
    private final YieldServices services;

    private double currentNormX;
    private double currentNormY;

    private boolean isDragging = false;
    private int dragOffsetX;
    private int dragOffsetY;
    private final YieldProject dummyProject;

    public HudEditorScreen(Screen parent, YieldServices services) {
        super(Component.literal("Yield HUD Editor"));
        this.parent = parent;
        this.services = services;
        this.currentNormX = Config.OVERLAY_X.get();
        this.currentNormY = Config.OVERLAY_Y.get();
        this.dummyProject = new YieldProject("Preview Project").withTrackXp(true);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("yield.label.save"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0x40000000, 0x40000000);
        gfx.drawCenteredString(this.font, "Drag the HUD to move. Press ESC to Cancel.", this.width / 2, 10, Theme.TEXT_PRIMARY);

        YieldProject displayProject = services.projectProvider().getActiveProject().orElse(dummyProject);

        int w = 150;
        int h = YieldOverlay.calculateHeight(displayProject);

        int x = (int) (this.width * currentNormX);
        int y = (int) (this.height * currentNormY);
        x = Mth.clamp(x, 0, this.width - w);
        y = Mth.clamp(y, 0, this.height - h);

        boolean isPaused = !services.sessionStatus().isRunning();
        YieldOverlay.renderHud(gfx, this.font, displayProject, x, y, w, h, isPaused, services);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            YieldProject displayProject = services.projectProvider().getActiveProject().orElse(dummyProject);
            int w = 150;
            int h = YieldOverlay.calculateHeight(displayProject);
            int x = Mth.clamp((int) (this.width * currentNormX), 0, this.width - w);
            int y = Mth.clamp((int) (this.height * currentNormY), 0, this.height - h);

            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                isDragging = true;
                dragOffsetX = (int) mouseX - x;
                dragOffsetY = (int) mouseY - y;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            YieldProject displayProject = services.projectProvider().getActiveProject().orElse(dummyProject);
            int w = 150;
            int h = YieldOverlay.calculateHeight(displayProject);
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;
            newX = Mth.clamp(newX, 0, this.width - w);
            newY = Mth.clamp(newY, 0, this.height - h);
            this.currentNormX = (double) newX / this.width;
            this.currentNormY = (double) newY / this.height;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        Config.OVERLAY_X.set(this.currentNormX);
        Config.OVERLAY_Y.set(this.currentNormY);
        Config.SPEC.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}