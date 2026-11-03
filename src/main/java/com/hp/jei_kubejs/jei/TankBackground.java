package com.hp.jei_kubejs.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

class TankBackground implements IDrawable {
    private final int width;
    private final int height;

    TankBackground(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        graphics.fill(xOffset, yOffset, xOffset + width, yOffset + height, 0xFF373737);
        graphics.fill(xOffset + 1, yOffset + 1, xOffset + width, yOffset + height, 0xFFFFFFFF);
        graphics.fill(xOffset + 1, yOffset + 1, xOffset + width - 1, yOffset + height - 1, 0xFF8B8B8B);
    }
}
