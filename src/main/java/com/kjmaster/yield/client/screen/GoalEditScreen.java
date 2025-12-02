package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServices;
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
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GoalEditScreen extends Screen {

    private final YieldDashboardScreen parent;
    private final ProjectGoal goal;
    private final YieldProject projectContext;
    private final YieldServices services;

    private EditBox amountInput;
    private Button strictButton;
    private Button configComponentsButton;

    private boolean strictState;
    private List<ResourceLocation> ignoredComponents;

    private LinearLayout layout;

    public GoalEditScreen(YieldDashboardScreen parent, ProjectGoal goal, YieldProject projectContext, YieldServices services) {
        super(Component.translatable("yield.label.set_goal_amount"));
        this.parent = parent;
        this.goal = goal;
        this.projectContext = projectContext;
        this.services = services;
        this.strictState = goal.strict();
        this.ignoredComponents = new ArrayList<>(goal.ignoredComponents());
    }

    @Override
    protected void init() {
        this.layout = LinearLayout.vertical().spacing(8);
        this.layout.defaultCellSetting().alignHorizontallyCenter();

        this.layout.addChild(new StringWidget(this.title, this.font));

        this.amountInput = new EditBox(this.font, 120, 20, Component.translatable("yield.label.amount"));
        this.amountInput.setValue(String.valueOf(goal.targetAmount()));
        this.amountInput.setFilter(s -> s.matches("\\d*"));
        this.layout.addChild(this.amountInput);

        // Strict Mode Row
        LinearLayout strictRow = LinearLayout.horizontal().spacing(4);

        this.strictButton = Button.builder(getStrictMessage(), btn -> {
            this.strictState = !this.strictState;
            btn.setMessage(getStrictMessage());
            this.configComponentsButton.active = this.strictState;
        }).width(100).build();

        this.configComponentsButton = Button.builder(Component.translatable("yield.label.components_btn"), btn -> {
            this.minecraft.setScreen(new ComponentSelectionScreen(this, goal.getRenderStack(), ignoredComponents, newList -> {
                this.ignoredComponents = newList;
            }));
        }).width(90).build();
        this.configComponentsButton.active = this.strictState;

        strictRow.addChild(this.strictButton);
        strictRow.addChild(this.configComponentsButton);
        this.layout.addChild(strictRow);

        // Action Row
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
        return Component.literal("Strict: " + status).withColor(color);
    }

    private void save() {
        try {
            int amount = Integer.parseInt(this.amountInput.getValue());
            int targetAmount = Math.max(1, amount);
            ProjectGoal updatedGoal = new ProjectGoal(
                    this.goal.id(),
                    this.goal.item(),
                    targetAmount,
                    this.strictState,
                    this.goal.components(),
                    this.goal.targetTag(),
                    this.ignoredComponents
            );

            YieldProject updatedProject = services.goalDomainService().updateGoal(this.projectContext, updatedGoal);

            // UI Updates via Events usually, but explicit refresh ensures snappy UX for the modal parent
            this.parent.updateUiState(updatedProject);
            this.services.projectController().updateProject(updatedProject);
        } catch (NumberFormatException ignored) {}
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