package com.kjmaster.yield.client.component;

import com.kjmaster.yield.api.IProjectController;
import com.kjmaster.yield.api.IProjectProvider;
import com.kjmaster.yield.api.ISessionStatus;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.screen.HudEditorScreen;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.event.internal.YieldEvents;
import com.kjmaster.yield.project.YieldProject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class ProjectSidebar extends AbstractWidget {

    private final Minecraft minecraft;
    private final Font font;

    // Dependencies
    private final IProjectProvider projectProvider;
    private final IProjectController projectController;
    private final ISessionStatus sessionStatus;
    private final YieldEventBus eventBus;

    private final ProjectList projectList;
    private final LinearLayout footerLayout;

    private Button newProjectButton;
    private Button xpToggleButton;
    private Button moveHudButton;

    private Consumer<YieldProject> onProjectSelected;

    public ProjectSidebar(Minecraft mc, int width, int height,
                          IProjectProvider projectProvider,
                          IProjectController projectController,
                          ISessionStatus sessionStatus,
                          YieldEventBus eventBus) {
        super(0, 0, width, height, Component.empty());
        this.minecraft = mc;
        this.font = mc.font;
        this.projectProvider = projectProvider;
        this.projectController = projectController;
        this.sessionStatus = sessionStatus;
        this.eventBus = eventBus;

        // Initialize List with dummy height; resized in setFixedSize
        this.projectList = new ProjectList(mc, width, height, 0, 24);

        this.footerLayout = LinearLayout.vertical().spacing(5);
        initFooter(mc.screen);
        registerEvents();
    }

    private void registerEvents() {
        // Rebuild list when projects are added/removed
        eventBus.register(YieldEvents.ProjectListChanged.class, event -> {
            this.refreshList();
        });

        // Update selection visualization when active project changes
        eventBus.register(YieldEvents.ActiveProjectChanged.class, event -> {
            this.updateWidgetStates();
        });

        // Listen for updates (name change, strict toggle) to refresh list visuals
        eventBus.register(YieldEvents.ProjectUpdated.class, event -> {
            updateWidgetStates();
            // We don't need to rebuild the list here, because the entries now look up data dynamically.
            // Just triggering a repaint (via updateWidgetStates or implied) is enough.
        });
    }

    private void initFooter(Screen parentScreen) {
        this.xpToggleButton = Button.builder(Component.literal("XP"), btn -> {
            YieldProject p = getSelectedProject();
            if (p != null) {
                YieldProject updated = p.withTrackXp(!p.trackXp());
                projectController.updateProject(updated);
            }
        }).width(Theme.SIDEBAR_WIDTH - 10).build();

        this.moveHudButton = Button.builder(Component.translatable("yield.label.move_hud"), btn -> {
            if (this.minecraft.screen != null) {
                this.minecraft.setScreen(new HudEditorScreen(this.minecraft.screen, projectProvider, sessionStatus));
            }
        }).width(Theme.SIDEBAR_WIDTH - 10).build();

        this.newProjectButton = Button.builder(Component.translatable("yield.label.new_project"), btn -> {
            projectController.createProject("New Project");
            if (!projectProvider.getProjects().isEmpty()) {
                YieldProject newP = projectProvider.getProjects().getLast();
                selectProject(newP);
                if (onProjectSelected != null) onProjectSelected.accept(newP);
            }
        }).width(Theme.SIDEBAR_WIDTH - 10).build();

        this.footerLayout.addChild(this.xpToggleButton);
        this.footerLayout.addChild(this.moveHudButton);
        this.footerLayout.addChild(this.newProjectButton);
    }

    public void setFixedSize(int width, int height) {
        this.setWidth(width);
        this.setHeight(height);

        this.footerLayout.arrangeElements();
        int layoutH = this.footerLayout.getHeight();
        int listHeight = height - layoutH - 10;

        this.projectList.updateSizeAndPosition(width, listHeight, this.getY());
        updateFooterPosition();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateFooterPosition();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.projectList.updateSizeAndPosition(this.width, this.projectList.getHeight(), y);
        updateFooterPosition();
    }

    private void updateFooterPosition() {
        int layoutH = this.footerLayout.getHeight();
        int footerY = this.getY() + this.getHeight() - layoutH - 5;
        this.footerLayout.setPosition(this.getX() + 5, footerY);
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        updateFooterPosition();

        gfx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Theme.SIDEBAR_BG);
        gfx.vLine(getX() + getWidth(), getY(), getY() + getHeight(), Theme.SIDEBAR_BORDER);

        this.projectList.render(gfx, mouseX, mouseY, partialTick);
        this.footerLayout.visitWidgets(w -> w.render(gfx, mouseX, mouseY, partialTick));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.projectList.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.projectList.mouseClicked(mouseX, mouseY, button)) return true;
        final boolean[] handled = {false};
        this.footerLayout.visitWidgets(w -> {
            if (!handled[0] && w.mouseClicked(mouseX, mouseY, button)) handled[0] = true;
        });
        return handled[0] || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public void setOnProjectSelected(Consumer<YieldProject> listener) { this.onProjectSelected = listener; }

    public void refreshList() {
        this.projectList.refresh();
        updateWidgetStates();
    }

    public void selectProject(YieldProject project) {
        this.projectList.selectProject(project);
        updateWidgetStates();
    }

    @Nullable
    public YieldProject getSelectedProject() {
        ProjectEntry entry = this.projectList.getSelected();
        // Resolve project dynamically to ensure freshness
        return entry != null ? entry.resolveProject() : null;
    }

    private void updateWidgetStates() {
        YieldProject p = getSelectedProject();
        boolean hasSel = (p != null);
        this.xpToggleButton.active = hasSel;
        if (hasSel) {
            String status = p.trackXp() ? "ON" : "OFF";
            int color = p.trackXp() ? 0xFF55FF55 : 0xFFAAAAAA;
            this.xpToggleButton.setMessage(Component.literal("Track XP: " + status).withColor(color));
        } else {
            this.xpToggleButton.setMessage(Component.literal("XP"));
        }
    }

    public class ProjectList extends ObjectSelectionList<ProjectEntry> {
        public ProjectList(Minecraft mc, int w, int h, int y, int itemH) { super(mc, w, h, y, itemH); }

        public void refresh() {
            this.clearEntries();
            for (YieldProject p : projectProvider.getProjects()) {
                this.addEntry(new ProjectEntry(p.id()));
            }
            this.setScrollAmount(0);
        }

        public void selectProject(YieldProject p) {
            if (p == null) { this.setSelected(null); return; }
            for (ProjectEntry entry : this.children()) {
                if (entry.projectId.equals(p.id())) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        @Override public int getRowWidth() { return this.width - 10; }
        @Override protected int getScrollbarPosition() { return this.width - 6; }
        @Override public void renderListBackground(@NotNull GuiGraphics g) {}
    }

    public class ProjectEntry extends ObjectSelectionList.Entry<ProjectEntry> {
        final UUID projectId;

        public ProjectEntry(UUID projectId) {
            this.projectId = projectId;
        }

        // Helper to get fresh data
        public YieldProject resolveProject() {
            return projectProvider.getProjects().stream()
                    .filter(p -> p.id().equals(projectId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public @NotNull Component getNarration() {
            YieldProject p = resolveProject();
            return p != null ? Component.literal(p.name()) : Component.empty();
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int idx, int top, int left, int width, int height, int mx, int my, boolean hover, float pt) {
            YieldProject project = resolveProject();
            if (project == null) return; // Project deleted but list not yet refreshed?

            boolean selected = projectList.getSelected() == this;
            if (selected) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_SEL_BG);
                gfx.fill(left, top, left + 2, top + height, Theme.LIST_ITEM_SEL_ACCENT);
            } else if (hover) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_HOVER);
            }

            Optional<YieldProject> active = projectProvider.getActiveProject();
            if (active.isPresent() && active.get().id().equals(project.id()) && sessionStatus.isRunning()) {
                int indicatorX = left + 4;
                int indicatorY = top + (height - 6) / 2;
                gfx.fill(indicatorX, indicatorY, indicatorX + 4, indicatorY + 4, Theme.ACTIVE_PROJECT_INDICATOR);
            }

            int color = selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            gfx.drawString(font, project.name(), left + 12, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                YieldProject project = resolveProject();
                if (project == null) return false;

                projectList.setSelected(this);
                // Visual Update
                ProjectSidebar.this.selectProject(project);

                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                // Pass FRESH project object to listener
                if (onProjectSelected != null) onProjectSelected.accept(project);

                return true;
            }
            return false;
        }
    }
}