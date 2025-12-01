package com.kjmaster.yield.client.screen;

import com.kjmaster.yield.client.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemSelectionScreen extends Screen {

    private final Screen parent;
    private final Consumer<ItemStack> onItemSelect;
    private final Consumer<TagKey<Item>> onTagSelect;

    private EditBox searchBox;
    private ItemList itemList;

    // Cache
    private final List<Item> allItems;
    private final List<TagKey<Item>> allTags;

    public ItemSelectionScreen(Screen parent, Consumer<ItemStack> onItemSelect, Consumer<TagKey<Item>> onTagSelect) {
        super(Component.translatable("yield.label.search_items"));
        this.parent = parent;
        this.onItemSelect = onItemSelect;
        this.onTagSelect = onTagSelect;

        // Cache Items
        this.allItems = BuiltInRegistries.ITEM.stream()
                .filter(i -> i != Items.AIR)
                .sorted(Comparator.comparing(item -> item.getDescription().getString()))
                .collect(Collectors.toList());

        // Cache Tags
        this.allTags = BuiltInRegistries.ITEM.getTags()
                .map(pair -> pair.getFirst())
                .sorted(Comparator.comparing(tag -> tag.location().toString()))
                .collect(Collectors.toList());
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.translatable("yield.label.search_items"));
        this.searchBox.setResponder(this::refreshList);
        this.addRenderableWidget(searchBox);

        this.itemList = new ItemList(this.minecraft, this.width, this.height, 50, this.height - 40);
        this.addRenderableWidget(itemList);

        this.addRenderableWidget(Button.builder(Component.translatable("yield.label.cancel"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20)
                .build());

        refreshList(this.searchBox.getValue());
        this.setInitialFocus(this.searchBox);
    }

    private void refreshList(String query) {
        if (this.itemList != null) {
            this.itemList.update(query);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);

        // Draw Hint
        if (this.searchBox.getValue().isEmpty()) {
            gfx.drawCenteredString(this.font, "Type '#' to search tags", this.width / 2, 8, Theme.TEXT_SECONDARY);
        } else {
            gfx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        }
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

    // --- Inner Classes ---

    class ItemList extends ObjectSelectionList<ItemRow> {
        private static final int SLOT_SIZE = 20;
        private static final int COLUMNS = 9;

        public ItemList(Minecraft mc, int width, int height, int y0, int y1) {
            super(mc, width, y1 - y0, y0, SLOT_SIZE + 2);
        }

        public void update(String query) {
            this.clearEntries();
            String lowerQuery = query.toLowerCase();
            boolean isTagSearch = lowerQuery.startsWith("#");

            if (isTagSearch) {
                updateTags(lowerQuery.substring(1)); // Remove '#'
            } else {
                updateItems(lowerQuery);
            }
        }

        private void updateItems(String query) {
            List<EntryWrapper> entries = allItems.stream()
                    .filter(item -> query.isEmpty() || new ItemStack(item).getHoverName().getString().toLowerCase().contains(query))
                    .limit(500) // Performance cap
                    .map(EntryWrapper::new)
                    .toList();
            buildRows(entries);
        }

        private void updateTags(String query) {
            List<EntryWrapper> entries = allTags.stream()
                    .filter(tag -> query.isEmpty() || tag.location().toString().contains(query))
                    .map(EntryWrapper::new)
                    .toList();
            buildRows(entries);
        }

        private void buildRows(List<EntryWrapper> entries) {
            List<EntryWrapper> currentRow = new ArrayList<>();
            for (EntryWrapper entry : entries) {
                currentRow.add(entry);
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

    // Wrapper to handle both Item and TagKey polymorphically for the list
    class EntryWrapper {
        final Item item; // Used for icon
        final Optional<ItemStack> stack;
        final Optional<TagKey<Item>> tag;

        // Constructor for Item
        EntryWrapper(Item item) {
            this.item = item;
            this.stack = Optional.of(new ItemStack(item));
            this.tag = Optional.empty();
        }

        // Constructor for Tag
        EntryWrapper(TagKey<Item> tag) {
            this.tag = Optional.of(tag);
            this.stack = Optional.empty();

            // Resolve a display item for the tag
            var optionalHolder = BuiltInRegistries.ITEM.getTag(tag)
                    .flatMap(holders -> holders.stream().findFirst());

            this.item = optionalHolder.map(holder -> holder.value()).orElse(Items.BARRIER);
        }
    }

    class ItemRow extends ObjectSelectionList.Entry<ItemRow> {
        private final List<EntryWrapper> entries;

        public ItemRow(List<EntryWrapper> entries) {
            this.entries = entries;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.empty();
        }

        @Override
        public void render(@NotNull GuiGraphics gfx, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            for (int i = 0; i < entries.size(); i++) {
                EntryWrapper entry = entries.get(i);
                int x = left + (i * ItemList.SLOT_SIZE);
                int y = top;

                boolean hoverSlot = mouseX >= x && mouseX < x + ItemList.SLOT_SIZE &&
                        mouseY >= y && mouseY < y + ItemList.SLOT_SIZE;

                if (hoverSlot) {
                    gfx.fill(x, y, x + ItemList.SLOT_SIZE, y + ItemList.SLOT_SIZE, 0x80FFFFFF);
                }

                ItemStack renderStack = new ItemStack(entry.item);
                gfx.renderItem(renderStack, x + 2, y + 2);

                // If it's a tag, draw a tiny "#" overlay
                if (entry.tag.isPresent()) {
                    gfx.pose().pushPose();
                    gfx.pose().translate(x + 10, y + 10, 200);
                    gfx.pose().scale(0.5f, 0.5f, 1f);
                    gfx.drawString(font, "#", 0, 0, 0xFF55FF55, true);
                    gfx.pose().popPose();
                }

                if (hoverSlot) {
                    if (entry.tag.isPresent()) {
                        gfx.renderTooltip(font, Component.literal(entry.tag.get().location().toString()), mouseX, mouseY);
                    } else {
                        gfx.renderTooltip(font, renderStack, mouseX, mouseY);
                    }
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int centeredLeft = ItemSelectionScreen.this.width / 2 - (ItemSelectionScreen.this.itemList.getRowWidth() / 2);
            double relX = mouseX - centeredLeft;

            if (relX >= 0) {
                int col = (int) (relX / ItemList.SLOT_SIZE);
                if (col >= 0 && col < entries.size()) {
                    EntryWrapper entry = entries.get(col);

                    if (entry.tag.isPresent()) {
                        ItemSelectionScreen.this.onTagSelect.accept(entry.tag.get());
                    } else {
                        ItemSelectionScreen.this.onItemSelect.accept(new ItemStack(entry.item));
                    }

                    ItemSelectionScreen.this.onClose();
                    return true;
                }
            }
            return false;
        }
    }
}