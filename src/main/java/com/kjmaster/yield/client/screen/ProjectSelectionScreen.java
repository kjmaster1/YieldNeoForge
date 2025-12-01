package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServiceRegistry;
import com.kjmaster.yield.api.IProjectManager;
import com.kjmaster.yield.api.ISessionTracker;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.SessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ProjectSelectionScreen extends Screen {

    private final Screen parent;
    private final ItemStack stackToTrack;
    private final List<YieldProject> projects;

    // Pagination
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    // Layout State for Background Rendering
    private int layoutX;
    private int layoutY;
    private int layoutWidth;
    private int layoutHeight;

    public ProjectSelectionScreen(Screen parent, ItemStack stackToTrack) {
        super(Component.translatable("yield.title.select_project"));
        this.parent = parent;
        this.stackToTrack = stackToTrack;
        this.projects = YieldServiceRegistry.getProjectManager().getProjects();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        repositionElements();
    }

    /**
     * Recalculates all positions and rebuilds widgets.
     */
    @Override
    protected void repositionElements() {
        int btnW = 140;
        int btnH = 20;
        int itemPadding = 4;

        // --- Calculate Dimensions ---
        // 1. Title Area: 25px
        // 2. List Area: (Items * (Height + Padding))
        // 3. Nav Area: 25px
        // 4. Cancel Button Area: 25px
        // 5. Padding: 20px (10 top, 10 bottom)

        int listHeight = ITEMS_PER_PAGE * (btnH + itemPadding);
        int contentHeight = 25 + listHeight + 25 + 25;

        this.layoutWidth = btnW + 40; // 20px padding on each side for breathing room
        this.layoutHeight = contentHeight + 20; // 10px padding top/bottom

        this.layoutX = (this.width - this.layoutWidth) / 2;
        this.layoutY = (this.height - this.layoutHeight) / 2;

        int centerX = this.width / 2;
        int currentY = this.layoutY + 25; // Start after Title space

        // --- 1. Project List Buttons ---
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(projects.size(), startIdx + ITEMS_PER_PAGE);

        for (int i = startIdx; i < endIdx; i++) {
            YieldProject project = projects.get(i);
            // Calculate specific Y for this button
            int rowY = currentY + ((i - startIdx) * (btnH + itemPadding));

            this.addRenderableWidget(Button.builder(Component.literal(project.getName()), btn -> {
                selectProject(project);
            }).bounds(centerX - (btnW / 2), rowY, btnW, btnH).build());
        }

        // --- 2. Navigation Buttons ---
        // Move Y pointer past the list
        int navY = currentY + listHeight + 5;
        int navBtnW = 50;
        int navGap = 40; // Gap between Prev and Next to fit Page Number

        // Previous
        Button prevBtn = this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                init(); // Re-init to refresh list
            }
        }).bounds(centerX - navBtnW - (navGap / 2), navY, navBtnW, 20).build());
        prevBtn.active = currentPage > 0;

        // Next
        int totalPages = (int) Math.ceil((double) projects.size() / ITEMS_PER_PAGE);
        Button nextBtn = this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                init(); // Re-init to refresh list
            }
        }).bounds(centerX + (navGap / 2), navY, navBtnW, 20).build());
        nextBtn.active = currentPage < totalPages - 1;

        // --- 3. Cancel Button ---
        // Move Y pointer past Nav row
        int cancelY = navY + 25;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX - 40, cancelY, 80, 20).build());
    }

    private void selectProject(YieldProject project) {
        // Add goal to the selected project
        project.addGoal(ProjectGoal.fromStack(stackToTrack, stackToTrack.getCount()));

        // Set active and start session
        YieldServiceRegistry.getProjectManager().setActiveProject(project);
        YieldServiceRegistry.getSessionTracker().startSession();

        // Play success sound and close
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1. Render Parent Screen (Z=0)
        if (this.parent != null) {
            this.parent.render(gfx, -1, -1, partialTick);
        }

        // 2. Background Layer (Z=400)
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);

        // Fullscreen Dim
        gfx.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xC0000000);

        // Content Box (Dark background + Outline)
        gfx.fill(layoutX, layoutY, layoutX + layoutWidth, layoutY + layoutHeight, 0xFF303030);
        gfx.renderOutline(layoutX, layoutY, layoutWidth, layoutHeight, 0xFFFFFFFF);

        gfx.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 1 & 2. Render Background Layers
        renderBackground(gfx, mouseX, mouseY, partialTick);

        // 3. Render Widgets & Text (Z=600)
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 600);

        // Draw Title inside the box
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.layoutY + 10, Theme.TEXT_PRIMARY);

        // Draw Page Number
        // Calculated to sit exactly in the gap between Prev/Next buttons
        int btnH = 20;
        int itemPadding = 4;
        int listHeight = ITEMS_PER_PAGE * (btnH + itemPadding);
        int navY = (this.layoutY + 25) + listHeight + 5;

        int totalPages = (int) Math.ceil((double) projects.size() / ITEMS_PER_PAGE);
        String pageStr = (currentPage + 1) + "/" + Math.max(1, totalPages);

        // +6 y-offset to center text vertically in the 20px height of the buttons
        gfx.drawCenteredString(this.font, pageStr, this.width / 2, navY + 6, Theme.TEXT_SECONDARY);

        // Render Widgets manually to ensure they are on this Z-layer
        for (Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        gfx.pose().popPose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}