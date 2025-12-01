package com.kjmaster.yield.client.component;

import com.kjmaster.yield.YieldServiceRegistry;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ProjectSidebar implements Renderable, GuiEventListener, NarratableEntry {

    private final Screen parentScreen;
    private final Minecraft minecraft;
    private final Font font;

    // Bounds
    private int x, y, width, height;

    // Widgets
    private ProjectList projectList;
    private Button newProjectButton;
    private Button xpToggleButton;
    private Button moveHudButton;
    private LinearLayout footerLayout;

    // Events
    private Consumer<YieldProject> onProjectSelected;

    public ProjectSidebar(Screen parentScreen, int width, int height) {
        this.parentScreen = parentScreen;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.width = width;
        this.height = height;
        this.x = 0;
        this.y = 0;

        initWidgets();
    }

    private void initWidgets() {
        // 1. Footer Buttons
        this.xpToggleButton = Button.builder(Component.literal("XP"), btn -> {
            YieldProject p = getSelectedProject();
            if (p != null) {
                p.setTrackXp(!p.shouldTrackXp());
                YieldServiceRegistry.getProjectManager().save();
                updateWidgetStates();
            }
        }).width(width - 10).build();

        this.moveHudButton = Button.builder(Component.translatable("yield.label.move_hud"), btn -> {
            this.minecraft.setScreen(new HudEditorScreen(this.parentScreen));
        }).width(width - 10).build();

        this.newProjectButton = Button.builder(Component.translatable("yield.label.new_project"), btn -> {
            YieldServiceRegistry.getProjectManager().createProject("New Project");
            this.refreshList();
            List<YieldProject> projects = YieldServiceRegistry.getProjectManager().getProjects();
            if (!projects.isEmpty()) {
                selectProject(projects.getLast());
            }
        }).width(width - 10).build();

        // 2. Layout Footer
        this.footerLayout = LinearLayout.vertical().spacing(5);
        this.footerLayout.addChild(this.xpToggleButton);
        this.footerLayout.addChild(this.moveHudButton);
        this.footerLayout.addChild(this.newProjectButton);

        // 3. Project List
        // List height will be calculated dynamically in layout
        this.projectList = new ProjectList(this.minecraft, width, height, 0, 24);
    }

    public void setOnProjectSelected(Consumer<YieldProject> listener) {
        this.onProjectSelected = listener;
    }

    public void layout(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;

        // Position Footer at bottom
        this.footerLayout.arrangeElements();
        int footerHeight = this.footerLayout.getHeight();
        int footerY = y + height - footerHeight - 5; // 5px padding from bottom
        this.footerLayout.setPosition(x + 5, footerY); // 5px padding from left

        // Position List above footer
        int listBottom = Math.max(y, footerY - 10); // Ensure we don't go negative

        // Update list bounds
        this.projectList.updateSizeAndPosition(this.width, listBottom, y);
        this.projectList.setPosition(x, y);
        // Explicitly set left position for correct centering of rows
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
            String status = p.shouldTrackXp() ? "ON" : "OFF";
            int color = p.shouldTrackXp() ? 0xFF55FF55 : 0xFFAAAAAA;
            this.xpToggleButton.setMessage(Component.literal("Track XP: " + status).withColor(color));
        } else {
            this.xpToggleButton.setMessage(Component.literal("XP"));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // FIX: Ensure rendering happens on top of background
        gfx.pose().pushPose();
        gfx.pose().translate(0.0f, 0.0f, 10.0f); // Slight Z-offset

        // Draw Background
        gfx.fill(x, y, x + width, y + height, Theme.SIDEBAR_BG);
        gfx.vLine(x + width, y, y + height, Theme.SIDEBAR_BORDER);

        this.projectList.render(gfx, mouseX, mouseY, partialTick);

        // Render buttons manually
        this.xpToggleButton.render(gfx, mouseX, mouseY, partialTick);
        this.moveHudButton.render(gfx, mouseX, mouseY, partialTick);
        this.newProjectButton.render(gfx, mouseX, mouseY, partialTick);

        // Render XP Stats
        if (YieldServiceRegistry.getSessionTracker().isRunning()) {
            YieldProject p = getSelectedProject();
            if (p != null && p.shouldTrackXp()) {
                int xpRate = (int) YieldServiceRegistry.getSessionTracker().getXpPerHour();

                // Calculate position relative to the XP Toggle Button
                int textY = this.xpToggleButton.getY() - 10;
                int centerX = this.x + (this.width / 2);

                gfx.drawCenteredString(this.font, Component.literal(xpRate + " XP/hr"), centerX, textY, Theme.COLOR_XP);
            }
        }

        gfx.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.projectList.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.xpToggleButton.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.moveHudButton.mouseClicked(mouseX, mouseY, button)) return true;
        return this.newProjectButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() { return false; }

    @Override
    public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}

    // --- Inner Classes for List ---

    class ProjectList extends ObjectSelectionList<ProjectEntry> {
        public ProjectList(Minecraft mc, int w, int h, int y, int itemH) {
            super(mc, w, h, y, itemH);
        }

        public void refresh() {
            this.clearEntries();
            for (YieldProject p : YieldServiceRegistry.getProjectManager().getProjects()) {
                this.addEntry(new ProjectEntry(p));
            }
            // Maintain scroll position if possible
            this.setScrollAmount(this.getScrollAmount());
        }

        public void selectProject(YieldProject p) {
            if (p == null) {
                this.setSelected(null);
                return;
            }
            for (ProjectEntry entry : this.children()) {
                if (entry.project.getId().equals(p.getId())) {
                    this.setSelected(entry);
                    return;
                }
            }
        }

        public void setLeftPos(int left) {
            // In some mappings/versions this might be needed to offset the content
            // AbstractSelectionList usually handles this via setX, but explicit override is safer
            super.setX(left);
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        protected void renderListBackground(@NotNull GuiGraphics g) {
            // Transparent to let sidebar bg show
        }
    }

    class ProjectEntry extends ObjectSelectionList.Entry<ProjectEntry> {
        final YieldProject project;

        public ProjectEntry(YieldProject p) {
            this.project = p;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(project.getName());
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int idx, int top, int left, int width, int height, int mx, int my, boolean hover, float pt) {
            boolean selected = projectList.getSelected() == this;
            if (selected) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_SEL_BG);
                gfx.fill(left, top, left + 2, top + height, Theme.LIST_ITEM_SEL_ACCENT);
            } else if (hover) {
                gfx.fill(left, top, left + width - 4, top + height, Theme.LIST_ITEM_HOVER);
            }

            // FIX: Check isRunning() to determine if the indicator should be shown
            Optional<YieldProject> active = YieldServiceRegistry.getProjectManager().getActiveProject();
            boolean isSessionRunning = YieldServiceRegistry.getSessionTracker().isRunning();

            if (active.isPresent() && active.get().getId().equals(project.getId()) && isSessionRunning) {
                int indicatorX = left + 4;
                int indicatorY = top + (height - 6) / 2;
                gfx.fill(indicatorX, indicatorY, indicatorX + 4, indicatorY + 4, Theme.ACTIVE_PROJECT_INDICATOR);
            }

            int color = selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            String name = project.getName();
            int padding = 12;
            int availableWidth = width - (padding * 2);
            if (font.width(name) > availableWidth) {
                name = font.plainSubstrByWidth(name, availableWidth - 10) + "...";
            }
            gfx.drawString(font, name, left + padding, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                projectList.setSelected(this);
                ProjectSidebar.this.selectProject(this.project); // Trigger callbacks
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
            return false;
        }
    }
}