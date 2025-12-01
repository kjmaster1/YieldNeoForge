package com.kjmaster.yield.compat.jei;

import com.kjmaster.yield.Yield;
import com.kjmaster.yield.client.Theme;
import com.kjmaster.yield.client.screen.YieldDashboardScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JeiPlugin
public class YieldJeiPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Yield.MODID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 1. GUI Properties (Main Layout)
        registration.addGuiScreenHandler(YieldDashboardScreen.class, YieldGuiProperties::new);

        // 2. GUI Exclusion Areas
        registration.addGlobalGuiHandler(new YieldGlobalGuiHandler());

        // 3. Ghost Ingredient Handler (Drag and Drop)
        registration.addGhostIngredientHandler(YieldDashboardScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> @NotNull List<Target<I>> getTargetsTyped(@NotNull YieldDashboardScreen screen, @NotNull ITypedIngredient<I> typedIngredient, boolean doStart) {
                List<Target<I>> targets = new ArrayList<>();
                Object innerIngredient = typedIngredient.getIngredient();

                if (!(innerIngredient instanceof ItemStack) || screen.getSelectedProject() == null || screen.getEditingGoal() != null) {
                    return targets;
                }

                targets.add(new Target<>() {
                    @Override
                    public @NotNull Rect2i getArea() {
                        // Drop Zone matches the middle Content area
                        int startX = Theme.SIDEBAR_WIDTH;
                        int topY = Theme.TOP_BAR_HEIGHT + 15;

                        int width = screen.getContentRight() - startX;
                        int height = screen.height - topY;

                        return new Rect2i(startX, topY, Math.max(1, width), Math.max(1, height));
                    }

                    @Override
                    public void accept(@NotNull I rawIngredient) {
                        if (rawIngredient instanceof ItemStack droppedStack) {
                            screen.handleJeiDrop(droppedStack);
                        }
                    }
                });
                return targets;
            }

            @Override
            public void onComplete() {
            }
        });
    }

    /**
     * Global Handler to define areas that JEI should avoid.
     * Since YieldDashboardScreen is not an AbstractContainerScreen, we use IGlobalGuiHandler
     * and manually check if the current screen is our dashboard.
     */
    private static class YieldGlobalGuiHandler implements IGlobalGuiHandler {
        @Override
        public @NotNull Collection<Rect2i> getGuiExtraAreas() {
            Screen screen = Minecraft.getInstance().screen;
            // Only provide exclusion areas if our screen is the one currently open
            if (screen instanceof YieldDashboardScreen yieldScreen) {
                return yieldScreen.getExclusionAreas();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Helper class to snapshot the GUI properties at the moment of creation.
     */
    private static class YieldGuiProperties implements IGuiProperties {
        private final Class<? extends Screen> screenClass;
        private final int guiLeft;
        private final int guiTop;
        private final int guiXSize;
        private final int guiYSize;
        private final int screenWidth;
        private final int screenHeight;

        public YieldGuiProperties(YieldDashboardScreen screen) {
            this.screenClass = YieldDashboardScreen.class;
            // Since YieldDashboardScreen is a full-screen overlay, Left/Top are 0
            this.guiLeft = 0;
            this.guiTop = Theme.TOP_BAR_HEIGHT;

            // Snapshot the dynamic values immediately
            // If screen.width is not yet initialized (0), fallback to standard width
            int w = screen.width;
            int h = screen.height;
            if (w <= 0) w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            if (h <= 0) h = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            this.screenWidth = w;
            this.screenHeight = h;

            this.guiYSize = h - Theme.TOP_BAR_HEIGHT;

            if (screen.width > 0) {
                this.guiXSize = screen.getContentRight();
            } else {
                this.guiXSize = Math.max(Theme.SIDEBAR_WIDTH + 100, w - Theme.JEI_WIDTH);
            }
        }

        @Override
        public @NotNull Class<? extends Screen> screenClass() {
            return screenClass;
        }

        @Override
        public int guiLeft() {
            return guiLeft;
        }

        @Override
        public int guiTop() {
            return guiTop;
        }

        @Override
        public int guiXSize() {
            return guiXSize;
        }

        @Override
        public int guiYSize() {
            return guiYSize;
        }

        @Override
        public int screenWidth() {
            return screenWidth;
        }

        @Override
        public int screenHeight() {
            return screenHeight;
        }
    }
}