package com.kjmaster.yield.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemSelectionScreen extends Screen {

    private final Screen parent;
    private final Consumer<ItemStack> onSelect;
    private EditBox searchBox;
    private ItemList itemList;
    private final List<Item> allItems;
    private Button cancelButton;

    public ItemSelectionScreen(Screen parent, Consumer<ItemStack> onSelect) {
        super(Component.translatable("yield.label.search_items"));
        this.parent = parent;
        this.onSelect = onSelect;

        // Cache all items from registry, excluding air, sorted by name
        this.allItems = BuiltInRegistries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .sorted(Comparator.comparing(item -> item.getDescription().getString()))
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.translatable("yield.label.search_items"));
        this.searchBox.setResponder(this::refreshList);
        this.addRenderableWidget(searchBox);

        this.itemList = new ItemList(this.minecraft, this.width, this.height, 50, this.height - 40);
        this.addRenderableWidget(itemList);

        this.cancelButton = this.addRenderableWidget(Button.builder(Component.translatable("yield.label.cancel"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        refreshList(this.searchBox.getValue());
        this.setInitialFocus(this.searchBox);
    }

    private void refreshList(String query) {
        if (this.itemList != null) {
            this.itemList.updateItems(query);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderTransparentBackground(guiGraphics);
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

    // --- Inner Classes for List ---

    class ItemList extends ObjectSelectionList<ItemRow> {
        private static final int SLOT_SIZE = 20;
        private static final int COLUMNS = 9;

        public ItemList(Minecraft mc, int width, int height, int y0, int y1) {
            super(mc, width, y1 - y0, y0, SLOT_SIZE + 2);
        }

        public void updateItems(String query) {
            this.clearEntries();
            String lowerQuery = query.toLowerCase();

            List<Item> filtered = allItems.stream()
                    .filter(item -> {
                        if (query.isEmpty()) return true;
                        ItemStack stack = new ItemStack(item);
                        return stack.getHoverName().getString().toLowerCase().contains(lowerQuery)
                                || BuiltInRegistries.ITEM.getKey(item).toString().contains(lowerQuery);
                    })
                    .toList();

            // Batch items into rows
            List<Item> currentRow = new ArrayList<>();
            for (Item item : filtered) {
                currentRow.add(item);
                if (currentRow.size() >= COLUMNS) {
                    this.addEntry(new ItemRow(new ArrayList<>(currentRow)));
                    currentRow.clear();
                }
            }
            if (!currentRow.isEmpty()) {
                this.addEntry(new ItemRow(new ArrayList<>(currentRow)));
            }
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return COLUMNS * SLOT_SIZE;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + (getRowWidth() / 2) + 10;
        }
    }

    class ItemRow extends ObjectSelectionList.Entry<ItemRow> {
        private final List<Item> items;

        public ItemRow(List<Item> items) {
            this.items = items;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.empty();
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                int x = left + (i * ItemList.SLOT_SIZE);
                int y = top;

                boolean hoverSlot = mouseX >= x && mouseX < x + ItemList.SLOT_SIZE &&
                        mouseY >= y && mouseY < y + ItemList.SLOT_SIZE;

                if (hoverSlot) {
                    gfx.fill(x, y, x + ItemList.SLOT_SIZE, y + ItemList.SLOT_SIZE, 0x80FFFFFF);
                }

                ItemStack stack = new ItemStack(item);
                gfx.renderItem(stack, x + 2, y + 2);

                if (hoverSlot) {
                    gfx.renderTooltip(font, stack, mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Calculate which slot was clicked
            ItemList list = ItemSelectionScreen.this.itemList;
            int left = list.getRowLeft(); // Access protected/helper via simplified logic if needed, but usually getRowLeft() is distinct.
            // Actually, ObjectSelectionList doesn't expose getRowLeft publicly easily, calculating locally:
            int centeredLeft = ItemSelectionScreen.this.width / 2 - (list.getRowWidth() / 2);

            double relX = mouseX - centeredLeft;
            if (relX >= 0) {
                int col = (int) (relX / ItemList.SLOT_SIZE);
                if (col >= 0 && col < items.size()) {
                    Item item = items.get(col);
                    ItemSelectionScreen.this.onSelect.accept(new ItemStack(item));
                    ItemSelectionScreen.this.onClose();
                    return true;
                }
            }
            return false;
        }
    }
}
