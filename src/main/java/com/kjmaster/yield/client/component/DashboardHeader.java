package com.kjmaster.yield.client.component;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionController;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.event.internal.YieldEvents;
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

public class DashboardHeader extends AbstractWidget {

    // Dependencies (Interface Segregation)
    private final IProjectController projectController;
    private final IProjectProvider projectProvider;
    private final ISessionController sessionController;
    private final ISessionStatus sessionStatus;
    private final YieldEventBus eventBus;

    private final Font font;
    private EditBox nameInput;
    private Button startStopButton;
    private Button addGoalButton;
    private Button deleteButton;
    private final LinearLayout buttonRow;

    private YieldProject currentProject;

    // Callbacks for screen transitions
    private Runnable onAddGoalClicked;

    public DashboardHeader(YieldServices services) {
        super(0, 0, 0, Theme.TOP_BAR_HEIGHT, Component.empty());
        this.projectController = services.projectController();
        this.projectProvider = services.projectProvider();
        this.sessionController = services.sessionController();
        this.sessionStatus = services.sessionStatus();
        this.eventBus = services.eventBus();
        this.font = Minecraft.getInstance().font;
        this.buttonRow = LinearLayout.horizontal().spacing(4);

        initWidgets();
        registerEvents();
    }

    private void registerEvents() {
        eventBus.register(YieldEvents.ActiveProjectChanged.class, event -> {
            this.setProject(event.newActiveProject());
        });

        // If the project itself updates (name change, goal added), refresh local state
        eventBus.register(YieldEvents.ProjectUpdated.class, event -> {
            if (currentProject != null && currentProject.id().equals(event.project().id())) {
                this.setProject(event.project());
            }
        });

        eventBus.register(YieldEvents.SessionStarted.class, e -> updateButtonStates());
        eventBus.register(YieldEvents.SessionStopped.class, e -> updateButtonStates());
    }

    private void initWidgets() {
        this.nameInput = new EditBox(this.font, 0, 0, 100, 20, Component.translatable("yield.label.project_name"));
        this.nameInput.setMaxLength(32);
        this.nameInput.setResponder(text -> {
            if (currentProject != null && !text.equals(currentProject.name())) {
                YieldProject updated = currentProject.withName(text);
                projectController.updateProject(updated);
            }
        });

        this.startStopButton = Button.builder(Component.translatable("yield.label.start"), btn -> {
            if (currentProject == null) return;
            if (sessionStatus.isRunning()) {
                sessionController.stopSession();
            } else {
                projectController.setActiveProject(currentProject);
                sessionController.startSession();
            }
        }).width(80).build();

        this.addGoalButton = Button.builder(Component.translatable("yield.label.add_goal"), btn -> {
            if (onAddGoalClicked != null) onAddGoalClicked.run();
        }).width(60).build();

        this.deleteButton = Button.builder(Component.translatable("yield.label.delete"), btn -> {
            if (currentProject != null) {
                projectController.deleteProject(currentProject);
            }
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
        this.nameInput.setX(this.getX());
        this.nameInput.setY(this.getY() + (this.getHeight() - 20) / 2);
        this.buttonRow.arrangeElements();
        this.buttonRow.setPosition(this.getX() + this.nameInput.getWidth() + gap, this.getY() + (this.getHeight() - this.buttonRow.getHeight()) / 2);
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        updateChildrenPositions();
        this.nameInput.render(gfx, mouseX, mouseY, partialTick);
        this.buttonRow.visitWidgets(w -> w.render(gfx, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.nameInput.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(true);
            return true;
        }
        final boolean[] handled = {false};
        this.buttonRow.visitWidgets(w -> {
            if (!handled[0] && w.mouseClicked(mouseX, mouseY, button)) handled[0] = true;
        });
        if (handled[0]) {
            this.setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (this.nameInput != null) this.nameInput.setFocused(focused);
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

    public void setOnAddGoalClicked(Runnable onAddGoal) {
        this.onAddGoalClicked = onAddGoal;
    }

    public void setProject(YieldProject project) {
        this.currentProject = project;
        if (project != null) {
            // Only update text if different to allow typing
            if (!nameInput.getValue().equals(project.name())) {
                nameInput.setValue(project.name());
            }
        } else {
            nameInput.setValue("");
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSel = (currentProject != null);
        this.nameInput.setVisible(hasSel);
        this.nameInput.setEditable(hasSel);
        this.startStopButton.active = hasSel;
        this.addGoalButton.active = hasSel;
        this.deleteButton.active = hasSel;

        if (hasSel && sessionStatus.isRunning()) {
            boolean isActive = false;
            var activeOpt = projectProvider.getActiveProject();
            if (activeOpt.isPresent() && activeOpt.get().id().equals(currentProject.id())) {
                isActive = true;
            }
            this.startStopButton.setMessage(isActive ? Component.translatable("yield.label.stop") : Component.translatable("yield.label.switch"));
        } else {
            this.startStopButton.setMessage(Component.translatable("yield.label.start"));
        }
    }
}