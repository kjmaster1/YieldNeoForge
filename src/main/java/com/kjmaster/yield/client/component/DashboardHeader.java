package com.kjmaster.yield.client.component;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;

public class DashboardHeader extends AbstractWidget {

    private final Minecraft minecraft;
    private final Font font;
    private final YieldServices services;

    private EditBox nameInput;
    private Button startStopButton;
    private Button addGoalButton;
    private Button deleteButton;
    private final LinearLayout buttonRow;

    private YieldProject currentProject;
    private Runnable onAddGoalClicked;
    private Runnable onDeleteProjectClicked;
    private Runnable onStartStopClicked;
    private Consumer<YieldProject> onProjectNameUpdated;

    public DashboardHeader(YieldServices services) {
        super(0, 0, 0, Theme.TOP_BAR_HEIGHT, Component.empty());
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.services = services;
        this.buttonRow = LinearLayout.horizontal().spacing(4);
        initWidgets();
    }

    private void initWidgets() {
        this.nameInput = new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name"));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            if (currentProject != null && !text.equals(currentProject.name())) {
                YieldProject updated = currentProject.withName(text);
                services.projectController().updateProject(updated);
                if (onProjectNameUpdated != null) {
                    onProjectNameUpdated.accept(updated);
                }
            }
        });

        this.startStopButton = Button.builder(Component.translatable("yield.label.start"), btn -> {
            if (onStartStopClicked != null) onStartStopClicked.run();
        }).width(80).build();

        this.addGoalButton = Button.builder(Component.translatable("yield.label.add_goal"), btn -> {
            if (onAddGoalClicked != null) onAddGoalClicked.run();
        }).width(60).build();

        this.deleteButton = Button.builder(Component.translatable("yield.label.delete"), btn -> {
            if (onDeleteProjectClicked != null) onDeleteProjectClicked.run();
        }).width(60).build();

        this.buttonRow.addChild(this.startStopButton);
        this.buttonRow.addChild(this.addGoalButton);
        this.buttonRow.addChild(this.deleteButton);
    }

    public void setFixedSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);
        this.arrangeElements();
    }

    public void arrangeElements() {
        int buttonsW = 80 + 60 + 60 + (4 * 2);
        int gap = 10;
        int availableForInput = Math.max(50, getWidth() - buttonsW - gap);

        this.nameInput.setWidth(availableForInput);

        // Update positions immediately
        updateChildrenPositions();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateChildrenPositions();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateChildrenPositions();
    }

    private void updateChildrenPositions() {
        int gap = 10;
        // Center Input vertically
        this.nameInput.setX(this.getX());
        this.nameInput.setY(this.getY() + (this.getHeight() - 20) / 2);

        // Position Button Row
        this.buttonRow.arrangeElements(); // Ensure button row internal layout is fresh
        this.buttonRow.setPosition(this.getX() + this.nameInput.getWidth() + gap, this.getY() + (this.getHeight() - this.buttonRow.getHeight()) / 2);
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Ensure positions are correct before rendering (handles dynamic layout updates)
        updateChildrenPositions();

        this.nameInput.render(gfx, mouseX, mouseY, partialTick);
        this.buttonRow.visitWidgets(w -> w.render(gfx, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. Check Input
        if (this.nameInput.mouseClicked(mouseX, mouseY, button)) {
            // CRITICAL: Ensure we override focus behavior
            this.setFocused(true);
            return true;
        }

        // 2. Check Buttons
        final boolean[] handled = {false};
        this.buttonRow.visitWidgets(w -> {
            if (!handled[0] && w.mouseClicked(mouseX, mouseY, button)) handled[0] = true;
        });

        if (handled[0]) {
            this.setFocused(true);
            return true;
        }

        // 3. Fallback to self (activates listeners attached to this widget directly, if any)
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Propagates focus changes to the inner EditBox.
     * Without this, the EditBox will not accept key inputs even if DashboardHeader is focused.
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (this.nameInput != null) {
            this.nameInput.setFocused(focused);
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.nameInput.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.nameInput.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.nameInput.updateNarration(narrationElementOutput);
    }

    public void setCallbacks(Runnable onStartStop, Runnable onAddGoal, Runnable onDelete) {
        this.onStartStopClicked = onStartStop;
        this.onAddGoalClicked = onAddGoal;
        this.onDeleteProjectClicked = onDelete;
    }

    public void setOnProjectNameUpdated(Consumer<YieldProject> listener) {
        this.onProjectNameUpdated = listener;
    }

    public void setProject(YieldProject project) {
        this.currentProject = project;
        if (project != null) {
            if (!nameInput.getValue().equals(project.name())) {
                nameInput.setValue(project.name());
            }
            updateButtonStates();
        } else {
            nameInput.setValue("");
        }
    }

    public void updateButtonStates() {
        boolean hasSel = (currentProject != null);
        this.nameInput.setVisible(hasSel);
        this.nameInput.setEditable(hasSel);
        this.startStopButton.active = hasSel;
        this.addGoalButton.active = hasSel;
        this.deleteButton.active = hasSel;

        if (hasSel && services.sessionStatus().isRunning()) {
            Optional<YieldProject> active = services.projectProvider().getActiveProject();
            boolean isActive = active.isPresent() && active.get().id().equals(currentProject.id());
            this.startStopButton.setMessage(isActive ? Component.translatable("yield.label.stop") : Component.translatable("yield.label.switch"));
        } else {
            this.startStopButton.setMessage(Component.translatable("yield.label.start"));
        }
    }
}