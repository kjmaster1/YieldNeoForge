package com.kjmaster.yield.client.screen;

import net.minecraft.client.renderer.Rect2i;
import java.util.ArrayList;
import java.util.List;

public class GridLayoutManager {
    private final List<Rect2i> cachedRects = new ArrayList<>();
    private int x, y, width, height, slotSize, gap;

    public void update(int x, int y, int width, int height, int slotSize, int gap, int itemCount) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.slotSize = slotSize;
        this.gap = gap;

        cachedRects.clear();
        calculateRects(itemCount);
    }

    private void calculateRects(int itemCount) {
        if (itemCount == 0) return;

        int cols = Math.max(1, width / (slotSize + gap));
        int col = 0;
        int row = 0;

        for (int i = 0; i < itemCount; i++) {
            int drawX = x + col * (slotSize + gap);
            int drawY = y + row * (slotSize + gap);

            // Wrap if exceeding width
            if (drawX + slotSize > x + width) {
                col = 0;
                row++;
                drawX = x;
                drawY = y + row * (slotSize + gap);
            }

            // Stop if exceeding height
            if (drawY + slotSize > y + height) break;

            cachedRects.add(new Rect2i(drawX, drawY, slotSize, slotSize));

            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
    }

    public List<Rect2i> getRects() {
        return cachedRects;
    }

    public int getIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < cachedRects.size(); i++) {
            Rect2i r = cachedRects.get(i);
            if (mouseX >= r.getX() && mouseX < r.getX() + r.getWidth() &&
                    mouseY >= r.getY() && mouseY < r.getY() + r.getHeight()) {
                return i;
            }
        }
        return -1;
    }
}