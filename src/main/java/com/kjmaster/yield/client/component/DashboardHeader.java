package com.kjmaster.yield.client.component;

import com.kjmaster.yield.YieldServiceRegistry;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DashboardHeader implements Renderable, GuiEventListener, NarratableEntry {

    private final Minecraft minecraft;
    private final Font font;

    // Bounds
    private int x, y, width, height;

    // Widgets
    private EditBox nameInput;
    private Button startStopButton;
    private Button addGoalButton;
    private Button deleteButton;
    private LinearLayout buttonLayout;

    // State
    private YieldProject currentProject;

    // Callbacks
    private Runnable onAddGoalClicked;
    private Runnable onDeleteProjectClicked;
    private Runnable onStartStopClicked;

    public DashboardHeader() {
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        initWidgets();
    }

    private void initWidgets() {
        this.nameInput = new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name"));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            if (currentProject != null && !text.equals(currentProject.getName())) {
                currentProject.setName(text);
                YieldServiceRegistry.getProjectManager().save();
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

        this.buttonLayout = LinearLayout.horizontal().spacing(4);
        this.buttonLayout.addChild(this.startStopButton);
        this.buttonLayout.addChild(this.addGoalButton);
        this.buttonLayout.addChild(this.deleteButton);
    }

    public void setCallbacks(Runnable onStartStop, Runnable onAddGoal, Runnable onDelete) {
        this.onStartStopClicked = onStartStop;
        this.onAddGoalClicked = onAddGoal;
        this.onDeleteProjectClicked = onDelete;
    }

    public void setProject(YieldProject project) {
        this.currentProject = project;
        if (project != null) {
            if (!nameInput.getValue().equals(project.getName())) {
                nameInput.setValue(project.getName());
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

        if (hasSel && YieldServiceRegistry.getSessionTracker().isRunning()) {
            Optional<YieldProject> active = YieldServiceRegistry.getProjectManager().getActiveProject();
            boolean isActive = active.isPresent() && active.get().getId().equals(currentProject.getId());
            this.startStopButton.setMessage(isActive ? Component.translatable("yield.label.stop") : Component.translatable("yield.label.switch"));
        } else {
            this.startStopButton.setMessage(Component.translatable("yield.label.start"));
        }
    }

    public void layout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // 1. Arrange Right-Side Buttons
        this.buttonLayout.arrangeElements();
        int buttonsWidth = this.buttonLayout.getWidth();

        // Align buttons to the right, but keep padding
        int buttonsX = x + width - buttonsWidth;
        int buttonsY = y + 8; // Top padding

        this.buttonLayout.setPosition(buttonsX, buttonsY);

        // 2. Arrange Name Input (Left side)
        // It fills the space between X and ButtonsX
        int inputAvailableWidth = buttonsX - x - 10; // 10px gap

        this.nameInput.setX(x);
        this.nameInput.setY(buttonsY);

        if (inputAvailableWidth < 50) {
            this.nameInput.setWidth(0);
            this.nameInput.setVisible(false);
        } else {
            this.nameInput.setWidth(inputAvailableWidth);
            this.nameInput.setVisible(currentProject != null);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (currentProject == null) return;

        this.nameInput.render(gfx, mouseX, mouseY, partialTick);
        this.startStopButton.render(gfx, mouseX, mouseY, partialTick);
        this.addGoalButton.render(gfx, mouseX, mouseY, partialTick);
        this.deleteButton.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentProject == null) return false;
        if (this.nameInput.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.startStopButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.addGoalButton.mouseClicked(mouseX, mouseY, button)) return true;
        return this.deleteButton.mouseClicked(mouseX, mouseY, button);
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
    public void setFocused(boolean focused) {}
    @Override
    public boolean isFocused() { return nameInput.isFocused(); }
    @Override
    public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
}