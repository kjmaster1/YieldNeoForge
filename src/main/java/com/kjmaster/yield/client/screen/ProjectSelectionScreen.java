package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
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
    private final YieldServices services;
    private final List<YieldProject> projects;

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    private int layoutX, layoutY, layoutWidth, layoutHeight;

    public ProjectSelectionScreen(Screen parent, ItemStack stackToTrack, YieldServices services) {
        super(Component.translatable("yield.title.select_project"));
        this.parent = parent;
        this.stackToTrack = stackToTrack;
        this.services = services;
        this.projects = services.projectProvider().getProjects();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        int btnW = 140;
        int btnH = 20;
        int itemPadding = 4;

        int listHeight = ITEMS_PER_PAGE * (btnH + itemPadding);
        int contentHeight = 25 + listHeight + 25 + 25;

        this.layoutWidth = btnW + 40;
        this.layoutHeight = contentHeight + 20;

        this.layoutX = (this.width - this.layoutWidth) / 2;
        this.layoutY = (this.height - this.layoutHeight) / 2;

        int centerX = this.width / 2;
        int currentY = this.layoutY + 25;

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(projects.size(), startIdx + ITEMS_PER_PAGE);

        for (int i = startIdx; i < endIdx; i++) {
            YieldProject project = projects.get(i);
            int rowY = currentY + ((i - startIdx) * (btnH + itemPadding));
            this.addRenderableWidget(Button.builder(Component.literal(project.name()), btn -> {
                selectProject(project);
            }).bounds(centerX - (btnW / 2), rowY, btnW, btnH).build());
        }

        int navY = currentY + listHeight + 5;
        int navBtnW = 50;
        int navGap = 40;

        Button prevBtn = this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                init();
            }
        }).bounds(centerX - navBtnW - (navGap / 2), navY, navBtnW, 20).build());
        prevBtn.active = currentPage > 0;

        int totalPages = (int) Math.ceil((double) projects.size() / ITEMS_PER_PAGE);
        Button nextBtn = this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                init();
            }
        }).bounds(centerX + (navGap / 2), navY, navBtnW, 20).build());
        nextBtn.active = currentPage < totalPages - 1;

        int cancelY = navY + 25;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX - 40, cancelY, 80, 20).build());
    }

    private void selectProject(YieldProject project) {
        YieldProject updated = services.goalDomainService().addGoal(project, ProjectGoal.fromStack(stackToTrack, stackToTrack.getCount()));
        services.projectController().updateProject(updated);
        services.projectController().setActiveProject(updated);
        services.sessionController().startSession();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null) {
            this.parent.render(gfx, -1, -1, partialTick);
        }
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);
        gfx.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xC0000000);
        gfx.fill(layoutX, layoutY, layoutX + layoutWidth, layoutY + layoutHeight, 0xFF303030);
        gfx.renderOutline(layoutX, layoutY, layoutWidth, layoutHeight, 0xFFFFFFFF);
        gfx.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 600);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.layoutY + 10, Theme.TEXT_PRIMARY);

        int btnH = 20;
        int itemPadding = 4;
        int listHeight = ITEMS_PER_PAGE * (btnH + itemPadding);
        int navY = (this.layoutY + 25) + listHeight + 5;

        int totalPages = (int) Math.ceil((double) projects.size() / ITEMS_PER_PAGE);
        String pageStr = (currentPage + 1) + "/" + Math.max(1, totalPages);
        gfx.drawCenteredString(this.font, pageStr, this.width / 2, navY + 6, Theme.TEXT_SECONDARY);

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