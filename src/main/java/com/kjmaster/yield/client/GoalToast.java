package com.kjmaster.yield.client;

import com.kjmaster.yield.project.ProjectGoal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GoalToast implements Toast {
    // Vanilla Toast Texture
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_TIME = 5000L; // Show for 5 seconds

    private final ProjectGoal goal;
    private boolean playedSound = false;

    public GoalToast(ProjectGoal goal) {
        this.goal = goal;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics gfx, ToastComponent toastComponent, long timeSinceLastVisible) {
        // 1. Draw Background
        gfx.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());

        // 2. Draw Item Icon
        gfx.renderItem(goal.getRenderStack(), 8, 8);

        // 3. Draw Text
        // Title: "Goal Complete!" (Gold)
        gfx.drawString(toastComponent.getMinecraft().font, Component.translatable("yield.toast.complete"), 30, 7, 0xFF500050, false);
        // Description: "64/64 Diamonds" (Dark Grey)
        String desc = goal.getTargetAmount() + " " + goal.getItem().getName(new ItemStack(goal.getItem())).getString();
        gfx.drawString(toastComponent.getMinecraft().font, desc, 30, 18, 0xFF000000, false);

        // 4. Play Sound (Once)
        if (!this.playedSound && timeSinceLastVisible > 0) {
            this.playedSound = true;
            // Use the "Challenge Complete" sound for extra satisfaction
            toastComponent.getMinecraft().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F)
            );
        }

        return timeSinceLastVisible >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }
}