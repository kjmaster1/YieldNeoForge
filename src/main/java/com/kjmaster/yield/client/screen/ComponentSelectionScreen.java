package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.client.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ComponentSelectionScreen extends Screen {

    private final Screen parent;
    private final ItemStack stack;
    private final List<ResourceLocation> ignoredComponents;
    private final Consumer<List<ResourceLocation>> onSave;

    private ComponentList componentList;

    public ComponentSelectionScreen(Screen parent, ItemStack stack, List<ResourceLocation> currentIgnored, Consumer<List<ResourceLocation>> onSave) {
        super(Component.translatable("yield.title.component_selection"));
        this.parent = parent;
        this.stack = stack;
        this.ignoredComponents = new ArrayList<>(currentIgnored); // Work on copy
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        this.componentList = new ComponentList(this.minecraft, this.width, this.height, 32, this.height - 32);
        this.addRenderableWidget(componentList);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            onSave.accept(ignoredComponents);
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 100, this.height - 24, 200, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 12, Theme.TEXT_PRIMARY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    class ComponentList extends ObjectSelectionList<ComponentEntry> {
        public ComponentList(Minecraft mc, int width, int height, int y0, int y1) {
            super(mc, width, height, y0, 24);
            refresh();
        }

        public void refresh() {
            this.clearEntries();
            for (TypedDataComponent<?> component : stack.getComponents()) {
                DataComponentType<?> type = component.type();
                ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
                if (id != null) {
                    this.addEntry(new ComponentEntry(id));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 140;
        }
    }

    class ComponentEntry extends ObjectSelectionList.Entry<ComponentEntry> {
        private final ResourceLocation id;
        private final Button toggleBtn;

        public ComponentEntry(ResourceLocation id) {
            this.id = id;

            boolean isIgnored = ignoredComponents.contains(id);
            Component statusText = isIgnored
                    ? Component.translatable("yield.label.ignored").withStyle(ChatFormatting.RED)
                    : Component.translatable("yield.label.tracked").withStyle(ChatFormatting.GREEN);

            this.toggleBtn = Button.builder(statusText, btn -> {
                if (ignoredComponents.contains(id)) {
                    ignoredComponents.remove(id);
                } else {
                    ignoredComponents.add(id);
                }

                // Refresh button text
                boolean nowIgnored = ignoredComponents.contains(id);
                Component newText = nowIgnored
                        ? Component.translatable("yield.label.ignored").withStyle(ChatFormatting.RED)
                        : Component.translatable("yield.label.tracked").withStyle(ChatFormatting.GREEN);
                btn.setMessage(newText);
            }).bounds(0, 0, 80, 20).build();
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            String name = id.toString();
            // Truncate if too long
            if (font.width(name) > 170) {
                name = font.plainSubstrByWidth(name, 160) + "...";
            }

            gfx.drawString(font, name, left + 5, top + 6, Theme.TEXT_PRIMARY, false);

            this.toggleBtn.setX(left + width - 85);
            this.toggleBtn.setY(top);
            this.toggleBtn.render(gfx, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.toggleBtn.mouseClicked(mouseX, mouseY, button)) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(id.toString());
        }
    }
}