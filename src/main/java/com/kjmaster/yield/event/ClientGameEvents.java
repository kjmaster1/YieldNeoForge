package com.kjmaster.yield.event;

import com.kjmaster.yield.Yield;
import com.kjmaster.yield.client.KeyBindings;
import com.kjmaster.yield.client.screen.YieldDashboardScreen;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.SessionTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Yield.MODID, value = Dist.CLIENT)
public class ClientGameEvents {

    // --- Modal State ---
    private static boolean isSelectingProject = false;
    private static int currentPage = 0; // Pagination State
    private static final int ITEMS_PER_PAGE = 6; // Limit per page

    private static ItemStack pendingStack = ItemStack.EMPTY;
    private static final List<ClickArea> clickAreas = new ArrayList<>();

    // Record for handling both Projects and Nav Buttons
    private interface ClickAction { void run(); }
    private record ClickArea(Rect2i area, ClickAction action) {}

    private static boolean pendingLoad = false;

    // --- Events ---

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        SessionTracker.get().onTick(mc.player);
        while (KeyBindings.OPEN_DASHBOARD.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new YieldDashboardScreen());
            else if (mc.screen instanceof YieldDashboardScreen) mc.setScreen(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pendingLoad = true;

        // Ensure session is clean
        SessionTracker.get().stopSession();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Execute the deferred load
        if (pendingLoad && event.getEntity() == Minecraft.getInstance().player) {
            ProjectManager.get().load();
            pendingLoad = false; // Prevent reloading on dimension change
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // Player left. Save current state (just in case) and clear memory.
        ProjectManager.get().save();
        ProjectManager.get().clear();
        SessionTracker.get().stopSession();
        pendingLoad = false;
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            SessionTracker.get().setDirty();
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            SessionTracker.get().setDirty();
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            SessionTracker.get().setDirty();
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onItemDestroy(PlayerDestroyItemEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            SessionTracker.get().addXpGain(event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        isSelectingProject = false;
        currentPage = 0; // Reset page on open
        pendingStack = ItemStack.EMPTY;
        clickAreas.clear();
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!isSelectingProject || pendingStack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        List<YieldProject> projects = ProjectManager.get().getProjects();
        if (projects.isEmpty()) {
            isSelectingProject = false;
            return;
        }

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 500);

        try {
            // Pagination Logic
            int totalPages = (int) Math.ceil((double) projects.size() / ITEMS_PER_PAGE);
            if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

            int startIdx = currentPage * ITEMS_PER_PAGE;
            int endIdx = Math.min(projects.size(), startIdx + ITEMS_PER_PAGE);
            int displayCount = endIdx - startIdx;

            // Dimensions
            int btnW = 140;
            int btnH = 20;
            int padding = 5;
            int headerH = 20;
            int navH = (totalPages > 1) ? 25 : 0; // Extra space for Nav buttons if needed

            int totalH = headerH + (displayCount * (btnH + padding)) + padding + navH;
            int boxW = btnW + (padding * 2);

            int startX = (screenW - boxW) / 2;
            int startY = (screenH - totalH) / 2;

            // Background
            gfx.fillGradient(0, 0, screenW, screenH, 0x80000000, 0x80000000);
            gfx.fill(startX, startY, startX + boxW, startY + totalH, 0xFF202020);
            gfx.renderOutline(startX, startY, boxW, totalH, 0xFFFFFFFF);
            gfx.drawCenteredString(mc.font, "Select Project", startX + boxW / 2, startY + 6, 0xFFFFFFFF);

            clickAreas.clear();
            int currentY = startY + headerH;

            double mx = mc.mouseHandler.xpos() * screenW / mc.getWindow().getWidth();
            double my = mc.mouseHandler.ypos() * screenH / mc.getWindow().getHeight();

            // Render Project Buttons
            for (int i = startIdx; i < endIdx; i++) {
                YieldProject p = projects.get(i);
                int btnX = startX + padding;

                boolean hovered = mx >= btnX && mx < btnX + btnW && my >= currentY && my < currentY + btnH;
                int color = hovered ? 0xFF404040 : 0xFF303030;

                gfx.fill(btnX, currentY, btnX + btnW, currentY + btnH, color);
                gfx.drawCenteredString(mc.font, p.getName(), btnX + btnW / 2, currentY + 6, 0xFFFFFFFF);

                // Add Click Action
                clickAreas.add(new ClickArea(new Rect2i(btnX, currentY, btnW, btnH), () -> {
                    addToProject(p, pendingStack);
                    finishSelection(p);
                }));

                currentY += btnH + padding;
            }

            // Render Navigation Buttons (if needed)
            if (totalPages > 1) {
                int navBtnW = 60;
                int navY = startY + totalH - 25;

                // Prev Button
                if (currentPage > 0) {
                    int prevX = startX + padding;
                    boolean hoverPrev = mx >= prevX && mx < prevX + navBtnW && my >= navY && my < navY + 20;
                    gfx.fill(prevX, navY, prevX + navBtnW, navY + 20, hoverPrev ? 0xFF505050 : 0xFF303030);
                    gfx.drawCenteredString(mc.font, "<", prevX + navBtnW / 2, navY + 6, 0xFFFFFFFF);

                    clickAreas.add(new ClickArea(new Rect2i(prevX, navY, navBtnW, 20), () -> {
                        currentPage--;
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }));
                }

                // Next Button
                if (currentPage < totalPages - 1) {
                    int nextX = startX + boxW - padding - navBtnW;
                    boolean hoverNext = mx >= nextX && mx < nextX + navBtnW && my >= navY && my < navY + 20;
                    gfx.fill(nextX, navY, nextX + navBtnW, navY + 20, hoverNext ? 0xFF505050 : 0xFF303030);
                    gfx.drawCenteredString(mc.font, ">", nextX + navBtnW / 2, navY + 6, 0xFFFFFFFF);

                    clickAreas.add(new ClickArea(new Rect2i(nextX, navY, navBtnW, 20), () -> {
                        currentPage++;
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }));
                }

                // Page Indicator
                String pageStr = (currentPage + 1) + "/" + totalPages;
                gfx.drawCenteredString(mc.font, pageStr, startX + boxW / 2, navY + 6, 0xFFAAAAAA);
            }

        } finally {
            gfx.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. Intercept Modal Clicks
        if (isSelectingProject) {
            double mx = event.getMouseX();
            double my = event.getMouseY();

            for (ClickArea area : clickAreas) {
                Rect2i r = area.area();
                if (mx >= r.getX() && mx < r.getX() + r.getWidth() && my >= r.getY() && my < r.getY() + r.getHeight()) {
                    area.action().run(); // Execute assigned action (Select Project OR Change Page)
                    event.setCanceled(true);
                    return;
                }
            }

            // Close if clicking outside
            isSelectingProject = false;
            event.setCanceled(true);
            return;
        }

        // 2. Handle Ctrl + Right Click
        boolean isTriggerModifierDown = isRawDown(KeyBindings.QUICK_TRACK);
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2 && isTriggerModifierDown) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                Slot slot = containerScreen.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    Optional<YieldProject> activeOpt = ProjectManager.get().getActiveProject();

                    if (activeOpt.isPresent() && SessionTracker.get().isRunning()) {
                        addToProject(activeOpt.get(), stack);
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    } else {
                        List<YieldProject> allProjects = ProjectManager.get().getProjects();
                        if (allProjects.isEmpty()) {
                            ProjectManager.get().createProject("New Project");
                            YieldProject newP = ProjectManager.get().getProjects().getLast();
                            ProjectManager.get().setActiveProject(newP);
                            addToProject(newP, stack);
                            SessionTracker.get().startSession();
                            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        } else {
                            isSelectingProject = true;
                            currentPage = 0; // Reset page
                            pendingStack = stack;
                        }
                    }
                    event.setCanceled(true);
                }
            }
        }
    }

    private static void finishSelection(YieldProject p) {
        isSelectingProject = false;
        ProjectManager.get().setActiveProject(p);
        SessionTracker.get().startSession();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static void addToProject(YieldProject project, ItemStack stack) {
        project.addGoal(ProjectGoal.fromStack(stack, stack.getCount()));
        ProjectManager.get().save();
    }

    private static boolean isRawDown(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else {
            return InputConstants.isKeyDown(window, key.getValue());
        }
    }
}