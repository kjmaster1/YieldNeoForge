package com.kjmaster.yield.client.component;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
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
import java.util.function.Consumer;

public class DashboardHeader implements Renderable, GuiEventListener, NarratableEntry {

    private final Minecraft minecraft;
    private final Font font;
    private final IProjectController projectController;
    private final ISessionStatus sessionStatus;
    private final IProjectProvider projectProvider;

    private int x, y, width, height;

    private EditBox nameInput;
    private Button startStopButton;
    private Button addGoalButton;
    private Button deleteButton;

    private LinearLayout rootLayout;

    private YieldProject currentProject;
    private Runnable onAddGoalClicked;
    private Runnable onDeleteProjectClicked;
    private Runnable onStartStopClicked;

    // NEW: Callback for name updates
    private Consumer<YieldProject> onProjectNameUpdated;

    public DashboardHeader(IProjectProvider projectProvider, IProjectController projectController, ISessionStatus sessionStatus) {
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.projectProvider = projectProvider;
        this.projectController = projectController;
        this.sessionStatus = sessionStatus;
        initWidgets();
    }

    private void initWidgets() {
        this.nameInput = new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name"));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            if (currentProject != null && !text.equals(currentProject.name())) {
                // 1. Create updated record
                YieldProject updated = currentProject.withName(text);

                // 2. Persist change
                projectController.updateProject(updated);

                // 3. Notify listener (Screen) to refresh sidebar
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

        this.rootLayout = LinearLayout.horizontal().spacing(10);
        this.rootLayout.addChild(this.nameInput);

        LinearLayout buttonRow = LinearLayout.horizontal().spacing(4);
        buttonRow.addChild(this.startStopButton);
        buttonRow.addChild(this.addGoalButton);
        buttonRow.addChild(this.deleteButton);

        this.rootLayout.addChild(buttonRow);
    }

    public void setCallbacks(Runnable onStartStop, Runnable onAddGoal, Runnable onDelete) {
        this.onStartStopClicked = onStartStop;
        this.onAddGoalClicked = onAddGoal;
        this.onDeleteProjectClicked = onDelete;
    }

    // NEW: Setter for the name update listener
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

        if (hasSel && sessionStatus.isRunning()) {
            Optional<YieldProject> active = projectProvider.getActiveProject();
            boolean isActive = active.isPresent() && active.get().id().equals(currentProject.id());
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

        this.rootLayout.setPosition(x, y + Theme.PADDING);

        int buttonRowWidth = 80 + 4 + 60 + 4 + 60;
        int padding = 20;
        int inputWidth = Math.max(50, width - buttonRowWidth - padding);

        this.nameInput.setWidth(inputWidth);
        this.nameInput.setHeight(20);

        this.rootLayout.arrangeElements();
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.nameInput.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
    public void setFocused(boolean focused) {
        nameInput.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return nameInput.isFocused();
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return nameInput.isFocused() ? NarrationPriority.FOCUSED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        nameInput.updateNarration(narrationElementOutput);
    }
}