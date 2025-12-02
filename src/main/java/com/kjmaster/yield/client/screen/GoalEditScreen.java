package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
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

    private final YieldDashboardScreen parent;
    private final ProjectGoal goal;
    private final YieldProject projectContext;
    private final IProjectController projectController;

    private EditBox amountInput;
    private Button strictButton;
    private boolean strictState;
    private LinearLayout layout;

    public GoalEditScreen(YieldDashboardScreen parent, ProjectGoal goal, YieldProject projectContext, IProjectController projectController) {
        super(Component.translatable("yield.label.set_goal_amount"));
        this.parent = parent;
        this.goal = goal;
        this.projectContext = projectContext;
        this.projectController = projectController;
        // Record accessor
        this.strictState = goal.strict();
    }

    @Override
    protected void init() {
        this.layout = LinearLayout.vertical().spacing(8);
        this.layout.defaultCellSetting().alignHorizontallyCenter();

        this.layout.addChild(new StringWidget(this.title, this.font));

        this.amountInput = new EditBox(this.font, 120, 20, Component.translatable("yield.label.amount"));
        // Record accessor
        this.amountInput.setValue(String.valueOf(goal.targetAmount()));
        this.amountInput.setFilter(s -> s.matches("\\d*"));
        this.layout.addChild(this.amountInput);

        this.strictButton = Button.builder(getStrictMessage(), btn -> {
            this.strictState = !this.strictState;
            btn.setMessage(getStrictMessage());
        }).width(120).build();
        this.layout.addChild(this.strictButton);

        LinearLayout buttonRow = LinearLayout.horizontal().spacing(10);
        buttonRow.addChild(Button.builder(Component.translatable("yield.label.save"), btn -> save()).width(55).build());
        buttonRow.addChild(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose()).width(55).build());
        this.layout.addChild(buttonRow);

        this.layout.arrangeElements();
        int x = (this.width - this.layout.getWidth()) / 2;
        int y = (this.height - this.layout.getHeight()) / 2;
        this.layout.setPosition(x, y);

        this.layout.visitWidgets(this::addRenderableWidget);
        this.setInitialFocus(this.amountInput);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null) {
            this.parent.render(gfx, -1, -1, partialTick);
        }
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 200);
        gfx.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xC0000000);

        if (this.layout != null) {
            int p = 10;
            int x = this.layout.getX() - p;
            int y = this.layout.getY() - p;
            int w = this.layout.getWidth() + (p * 2);
            int h = this.layout.getHeight() + (p * 2);
            gfx.fill(x, y, x + w, y + h, 0xFF303030);
            gfx.renderOutline(x, y, w, h, 0xFFFFFFFF);
        }
        gfx.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 300);
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
            int targetAmount = Math.max(1, amount);

            // 1. Create updated Goal record (preserve ID and static properties)
            ProjectGoal updatedGoal = new ProjectGoal(
                    this.goal.id(),
                    this.goal.item(),
                    targetAmount,
                    this.strictState,
                    this.goal.components(),
                    this.goal.targetTag()
            );

            // 2. Create updated Project record containing the new goal
            YieldProject updatedProject = this.projectContext.updateGoal(updatedGoal);

            this.parent.updateUiState(updatedProject);

            // 3. Persist the project update
            this.projectController.updateProject(updatedProject);

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