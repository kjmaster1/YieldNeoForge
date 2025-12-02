package com.kjmaster.yield.client.component;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.screen.HudEditorScreen;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class ProjectSidebar implements Renderable, GuiEventListener, NarratableEntry {

    private final Screen parentScreen;
    private final Minecraft minecraft;
    private final Font font;

    // Dependencies
    private final IProjectProvider projectProvider;
    private final IProjectController projectController;
    private final ISessionStatus sessionStatus;

    private int x, y, width, height;
    private ProjectList projectList;
    private Button newProjectButton;
    private Button xpToggleButton;
    private Button moveHudButton;
    private LinearLayout footerLayout;
    private Consumer<YieldProject> onProjectSelected;

    public ProjectSidebar(Screen parentScreen, int width, int height, IProjectProvider provider, IProjectController controller, ISessionStatus session) {
        this.parentScreen = parentScreen;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.width = width;
        this.height = height;
        this.projectProvider = provider;
        this.projectController = controller;
        this.sessionStatus = session;
        initWidgets();
    }

    private void initWidgets() {
        this.xpToggleButton = Button.builder(Component.literal("XP"), btn -> {
            YieldProject p = getSelectedProject();
            if (p != null) {
                // Immutable update: Create new instance -> Update via Controller
                YieldProject updated = p.withTrackXp(!p.trackXp());
                projectController.updateProject(updated);

                // Refresh to reflect changes (as the list might need to update the object reference)
                this.refreshList();
                this.selectProject(updated);
            }
        }).width(width - 10).build();

        this.moveHudButton = Button.builder(Component.translatable("yield.label.move_hud"), btn -> {
            this.minecraft.setScreen(new HudEditorScreen(this.parentScreen, projectProvider, sessionStatus));
        }).width(width - 10).build();

        this.newProjectButton = Button.builder(Component.translatable("yield.label.new_project"), btn -> {
            projectController.createProject("New Project");
            this.refreshList();
            if (!projectProvider.getProjects().isEmpty()) {
                selectProject(projectProvider.getProjects().getLast());
            }
        }).width(width - 10).build();

        this.footerLayout = LinearLayout.vertical().spacing(5);
        this.footerLayout.addChild(this.xpToggleButton);
        this.footerLayout.addChild(this.moveHudButton);
        this.footerLayout.addChild(this.newProjectButton);

        this.projectList = new ProjectList(this.minecraft, width, height, 0, 24);
    }

    public void setOnProjectSelected(Consumer<YieldProject> listener) {
        this.onProjectSelected = listener;
    }

    public void layout(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.footerLayout.arrangeElements();
        int footerHeight = this.footerLayout.getHeight();
        int footerY = y + height - footerHeight - 5;
        this.footerLayout.setPosition(x + 5, footerY);
        int listBottom = Math.max(y, footerY - 10);
        this.projectList.updateSizeAndPosition(this.width, listBottom, y);
        this.projectList.setPosition(x, y);
        this.projectList.setLeftPos(x);
    }

    public void refreshList() {
        this.projectList.refresh();
        updateWidgetStates();
    }

    public void selectProject(YieldProject project) {
        this.projectList.selectProject(project);
        updateWidgetStates();
        if (onProjectSelected != null) {
            onProjectSelected.accept(project);
        }
    }

    @Nullable
    public YieldProject getSelectedProject() {
        ProjectEntry entry = this.projectList.getSelected();
        return entry != null ? entry.project : null;
    }

    public void updateWidgetStates() {
        YieldProject p = getSelectedProject();
        boolean hasSel = (p != null);
        this.xpToggleButton.active = hasSel;
        if (hasSel) {
            // Record accessor: .trackXp() instead of .shouldTrackXp()
            String status = p.trackXp() ? "ON" : "OFF";
            int color = p.trackXp() ? 0xFF55FF55 : 0xFFAAAAAA;
            this.xpToggleButton.setMessage(Component.literal("Track XP: " + status).withColor(color));
        } else {
            this.xpToggleButton.setMessage(Component.literal("XP"));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.pose().pushPose();
        gfx.pose().translate(0.0f, 0.0f, 10.0f);
        gfx.fill(x, y, x + width, y + height, Theme.SIDEBAR_BG);
        gfx.vLine(x + width, y, y + height, Theme.SIDEBAR_BORDER);
        this.projectList.render(gfx, mouseX, mouseY, partialTick);
        this.xpToggleButton.render(gfx, mouseX, mouseY, partialTick);
        this.moveHudButton.render(gfx, mouseX, mouseY, partialTick);
        this.newProjectButton.render(gfx, mouseX, mouseY, partialTick);

        if (sessionStatus.isRunning()) {
            YieldProject p = getSelectedProject();
            // Record accessor: .trackXp()
            if (p != null && p.trackXp()) {
                int xpRate = (int) sessionStatus.getXpPerHour();
                int textY = this.xpToggleButton.getY() - 10;
                int centerX = this.x + (this.width / 2);
                gfx.drawCenteredString(this.font, Component.literal(xpRate + " XP/hr"), centerX, textY, Theme.COLOR_XP);
            }
        }
        gfx.pose().popPose();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.projectList.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.xpToggleButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.moveHudButton.mouseClicked(mouseX, mouseY, button)) return true;
        return this.newProjectButton.mouseClicked(mouseX, mouseY, button);
    }
    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

    class ProjectList extends ObjectSelectionList<ProjectEntry> {
        public ProjectList(Minecraft mc, int w, int h, int y, int itemH) { super(mc, w, h, y, itemH); }
        public void refresh() {
            this.clearEntries();
            for (YieldProject p : projectProvider.getProjects()) {
                this.addEntry(new ProjectEntry(p));
            }
            this.setScrollAmount(this.getScrollAmount());
        }
        public void selectProject(YieldProject p) {
            if (p == null) { this.setSelected(null); return; }
            for (ProjectEntry entry : this.children()) {
                // Record accessor: .id()
                if (entry.project.id().equals(p.id())) { this.setSelected(entry); return; }
            }
        }
        public void setLeftPos(int left) { super.setX(left); }
        @Override public int getRowWidth() { return this.width - 10; }
        @Override protected int getScrollbarPosition() { return this.width - 6; }
        @Override protected void renderListBackground(@NotNull GuiGraphics g) {}
    }

    class ProjectEntry extends ObjectSelectionList.Entry<ProjectEntry> {
        final YieldProject project;
        public ProjectEntry(YieldProject p) { this.project = p; }
        @Override public @NotNull Component getNarration() { return Component.literal(project.name()); } // Record accessor: .name()
        @Override public void render(@NotNull GuiGraphics gfx, int idx, int top, int left, int width, int height, int mx, int my, boolean hover, float pt) {
            boolean selected = projectList.getSelected() == this;
            if (selected) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_SEL_BG);
                gfx.fill(left, top, left + 2, top + height, Theme.LIST_ITEM_SEL_ACCENT);
            } else if (hover) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_HOVER);
            }

            Optional<YieldProject> active = projectProvider.getActiveProject();
            boolean isSessionRunning = sessionStatus.isRunning();

            // Record accessor: .id()
            if (active.isPresent() && active.get().id().equals(project.id()) && isSessionRunning) {
                int indicatorX = left + 4;
                int indicatorY = top + (height - 6) / 2;
                gfx.fill(indicatorX, indicatorY, indicatorX + 4, indicatorY + 4, Theme.ACTIVE_PROJECT_INDICATOR);
            }
            int color = selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            String name = project.name(); // Record accessor: .name()
            gfx.drawString(font, name, left + 12, top + (height - 9) / 2, color, false);
        }
        @Override public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                projectList.setSelected(this);
                ProjectSidebar.this.selectProject(this.project);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return false;
        }
    }
}