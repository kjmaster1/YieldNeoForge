package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServiceRegistry;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class GoalEditScreen extends Screen {

    private final Screen parent;
    private final ProjectGoal goal;

    private EditBox amountInput;
    private Button strictButton;
    private boolean strictState;

    // Store layout to render background box behind it
    private LinearLayout layout;

    public GoalEditScreen(Screen parent, ProjectGoal goal) {
        super(Component.translatable("yield.label.set_goal_amount"));
        this.parent = parent;
        this.goal = goal;
        this.strictState = goal.isStrict();
    }

    @Override
    protected void init() {
        // Use a Vertical Layout to stack elements automatically
        this.layout = LinearLayout.vertical().spacing(8);
        this.layout.defaultCellSetting().alignHorizontallyCenter();

        // Title
        this.layout.addChild(new StringWidget(this.title, this.font));

        // Amount Input
        this.amountInput = new EditBox(this.font, 120, 20, Component.translatable("yield.label.amount"));
        this.amountInput.setValue(String.valueOf(goal.getTargetAmount()));
        this.amountInput.setFilter(s -> s.matches("\\d*")); // Only digits
        this.layout.addChild(this.amountInput);

        // Strict Toggle
        this.strictButton = Button.builder(getStrictMessage(), btn -> {
            this.strictState = !this.strictState;
            btn.setMessage(getStrictMessage());
        }).width(120).build();
        this.layout.addChild(this.strictButton);

        // Button Row (Save/Cancel)
        LinearLayout buttonRow = LinearLayout.horizontal().spacing(10);
        buttonRow.addChild(Button.builder(Component.translatable("yield.label.save"), btn -> save()).width(55).build());
        buttonRow.addChild(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose()).width(55).build());
        this.layout.addChild(buttonRow);

        // Position the layout in the center of the screen
        this.layout.arrangeElements();
        int x = (this.width - this.layout.getWidth()) / 2;
        int y = (this.height - this.layout.getHeight()) / 2;
        this.layout.setPosition(x, y);

        this.layout.visitWidgets(this::addRenderableWidget);

        this.setInitialFocus(this.amountInput);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1. Render Parent (The Dashboard) at Z=0
        if (this.parent != null) {
            this.parent.render(gfx, -1, -1, partialTick);
        }

        // 2. Render Dim Layer & Box at Z=200
        // This ensures it covers the parent's items (which render around Z=150)
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 200);

        gfx.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xC0000000);

        // Render Modal Background Box
        if (this.layout != null) {
            int p = 10; // Padding
            int x = this.layout.getX() - p;
            int y = this.layout.getY() - p;
            int w = this.layout.getWidth() + (p * 2);
            int h = this.layout.getHeight() + (p * 2);

            // Dark Box with lighter outline
            gfx.fill(x, y, x + w, y + h, 0xFF303030);
            gfx.renderOutline(x, y, w, h, 0xFFFFFFFF);
        }

        gfx.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1. Render Background logic (Z=0 to Z=200)
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        // 2. Render Widgets at Z=300
        // We manually iterate renderables to control the Z-depth relative to the background
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 300); // 300 > 200 (Background). Widgets are now visible.

        for (Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        gfx.pose().popPose();
    }

    private Component getStrictMessage() {
        String status = strictState ? "ON" : "OFF";
        int color = strictState ? 0xFF55FF55 : Theme.TEXT_SECONDARY;
        return Component.literal("Strict Mode: " + status).withColor(color);
    }

    private void save() {
        try {
            int amount = Integer.parseInt(this.amountInput.getValue());
            this.goal.setTargetAmount(Math.max(1, amount));
            this.goal.setStrict(this.strictState);
            YieldServiceRegistry.getProjectManager().save();
        } catch (NumberFormatException ignored) {
        }
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}