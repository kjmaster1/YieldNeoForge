package com.kjmaster.yield.event;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.Yield;
import com.kjmaster.yield.client.KeyBindings;
import com.kjmaster.yield.client.screen.ProjectSelectionScreen;
import com.kjmaster.yield.client.screen.YieldDashboardScreen;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.kjmaster.yield.tracker.SessionTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Yield.MODID, value = Dist.CLIENT)
public class ClientGameEvents {

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

        while (KeyBindings.TOGGLE_OVERLAY.consumeClick()) {
            boolean newState = !Config.OVERLAY_ENABLED.get();
            Config.OVERLAY_ENABLED.set(newState);
            Config.SPEC.save();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pendingLoad = true;
        SessionTracker.get().stopSession();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (pendingLoad && event.getEntity() == Minecraft.getInstance().player) {
            ProjectManager.get().load();
            pendingLoad = false;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ProjectManager.get().save();
        ProjectManager.get().clear();
        SessionTracker.get().stopSession();
        pendingLoad = false;
    }

    // --- Tracking Triggers (Dirty State) ---
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() == Minecraft.getInstance().player) SessionTracker.get().setDirty();
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
    public static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Handle Ctrl + Right Click (Quick Track)
        boolean isTriggerModifierDown = isRawDown(KeyBindings.QUICK_TRACK);
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2 && isTriggerModifierDown) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                Slot slot = containerScreen.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    handleQuickTrack(mc, stack);
                    event.setCanceled(true);
                }
            }
        }
    }

    private static void handleQuickTrack(Minecraft mc, ItemStack stack) {
        Optional<YieldProject> activeOpt = ProjectManager.get().getActiveProject();

        // 1. If we have an active session, add immediately
        if (activeOpt.isPresent() && SessionTracker.get().isRunning()) {
            addToProject(activeOpt.get(), stack);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }

        // 2. No active session. Check if we need to create a default project or let user select.
        List<YieldProject> allProjects = ProjectManager.get().getProjects();
        if (allProjects.isEmpty()) {
            // Case A: No projects exist. Create one automatically.
            ProjectManager.get().createProject("New Project");
            YieldProject newP = ProjectManager.get().getProjects().getLast();
            ProjectManager.get().setActiveProject(newP);

            addToProject(newP, stack);
            SessionTracker.get().startSession();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } else {
            // Case B: Projects exist. Open selection screen to let user choose.
            // We pass the CURRENT screen as parent so we can render it in the background.
            mc.setScreen(new ProjectSelectionScreen(mc.screen, stack));
        }
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