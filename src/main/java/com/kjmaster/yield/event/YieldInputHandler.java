package com.kjmaster.yield.event;

import com.kjmaster.yield.Config;
import com.kjmaster.yield.YieldServices;
import com.kjmaster.yield.client.KeyBindings;
import com.kjmaster.yield.client.screen.ProjectSelectionScreen;
import com.kjmaster.yield.client.screen.YieldDashboardScreen;
import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

public class YieldInputHandler {

    private final YieldServices services;

    public YieldInputHandler(YieldServices services) {
        this.services = services;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        // Only handle PRESS actions (ignore release/repeat)
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Check against KeyMappings
        // We use matches() to respect the user's configured keys/mouse buttons
        if (KeyBindings.OPEN_DASHBOARD.matches(event.getKey(), event.getScanCode())) {
            toggleDashboard(mc);
        }

        if (KeyBindings.TOGGLE_OVERLAY.matches(event.getKey(), event.getScanCode())) {
            boolean newState = !Config.OVERLAY_ENABLED.get();
            Config.OVERLAY_ENABLED.set(newState);
            Config.SPEC.save();
        }
    }

    private void toggleDashboard(Minecraft mc) {
        if (mc.screen == null) {
            mc.setScreen(new YieldDashboardScreen(services));
        } else if (mc.screen instanceof YieldDashboardScreen) {
            mc.setScreen(null);
        }
    }

    @SubscribeEvent
    public void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // "Quick Track" logic remains here because it interacts with Screen Slots (Mouse),
        // but we verify the modifier key is held down.
        boolean isTriggerModifierDown = isRawDown(KeyBindings.QUICK_TRACK);

        // Check for Right Click + Modifier
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_2 && isTriggerModifierDown) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                Slot slot = containerScreen.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    handleQuickTrack(mc, stack);
                    event.setCanceled(true); // Consume the click so we don't split the stack
                }
            }
        }
    }

    private void handleQuickTrack(Minecraft mc, ItemStack stack) {
        Optional<YieldProject> activeOpt = services.projectProvider().getActiveProject();

        if (activeOpt.isPresent() && services.sessionStatus().isRunning()) {
            addToProject(activeOpt.get(), stack);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }

        List<YieldProject> allProjects = services.projectProvider().getProjects();
        if (allProjects.isEmpty()) {
            services.projectController().createProject("New Project");
            // Refresh list from provider to get the new instance
            if (!services.projectProvider().getProjects().isEmpty()) {
                YieldProject newP = services.projectProvider().getProjects().getLast();
                services.projectController().setActiveProject(newP);
                addToProject(newP, stack);
                services.sessionController().startSession();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        } else {
            mc.setScreen(new ProjectSelectionScreen(mc.screen, stack, services));
        }
    }

    private void addToProject(YieldProject project, ItemStack stack) {
        ProjectGoal goal = ProjectGoal.fromStack(stack, stack.getCount());
        YieldProject updated = services.goalDomainService().addGoal(project, goal);
        services.projectController().updateProject(updated);
    }

    private boolean isRawDown(KeyMapping mapping) {
        // Direct GLFW check is reliable for modifiers held during mouse clicks
        InputConstants.Key key = mapping.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else {
            return InputConstants.isKeyDown(window, key.getValue());
        }
    }
}