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
    // Vanilla Toast Sprite
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_TIME = 5000L;

    private final ProjectGoal goal;
    private boolean playedSound = false;

    public GoalToast(ProjectGoal goal) {
        this.goal = goal;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics gfx, ToastComponent toastComponent, long timeSinceLastVisible) {
        // 1. Draw Background
        gfx.blitSprite(TEXTURE, 0, 0, this.width(), this.height());

        // 2. Draw Item Icon
        gfx.renderItem(goal.getRenderStack(), 8, 8);

        // 3. Draw Text
        // Title: "Goal Met!" -> Vanilla Yellow (0xFFFFFF00)
        // We use 0xFFFFFF00 (ARGB) which matches vanilla's 16776960
        gfx.drawString(toastComponent.getMinecraft().font, Component.translatable("yield.toast.complete"), 30, 7, 0xFFFFFF00, false);

        // Description: "64/64 Diamonds" -> Vanilla White (0xFFFFFFFF)
        String desc = goal.getTargetAmount() + " " + goal.getItem().getName(new ItemStack(goal.getItem())).getString();

        // Handle long item names by truncating or letting the font renderer handle it (vanilla usually splits, but truncating is safer for simple implementation)
        int maxWidth = this.width() - 30 - 5;
        if (toastComponent.getMinecraft().font.width(desc) > maxWidth) {
            desc = toastComponent.getMinecraft().font.plainSubstrByWidth(desc, maxWidth - 10) + "...";
        }

        gfx.drawString(toastComponent.getMinecraft().font, desc, 30, 18, 0xFFFFFFFF, false);

        // 4. Play Sound (Once)
        if (!this.playedSound && timeSinceLastVisible > 0) {
            this.playedSound = true;
            toastComponent.getMinecraft().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F)
            );
        }

        return timeSinceLastVisible >= DISPLAY_TIME ? Visibility.HIDE : Visibility.SHOW;
    }
}