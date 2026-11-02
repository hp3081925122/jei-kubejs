package com.hp.jei_kubejs.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * onDraw / onTooltip 回调拿到的上下文。坐标相对分类左上角。
 * onDraw 每帧调用，回调里不要做耗时操作。
 */
public class DrawContext {
    private final RecipeData recipe;
    private final GuiGraphics graphics;
    private final double mouseX;
    private final double mouseY;
    private final List<Component> tooltipLines;

    public DrawContext(RecipeData recipe, GuiGraphics graphics, double mouseX, double mouseY, List<Component> tooltipLines) {
        this.recipe = recipe;
        this.graphics = graphics;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.tooltipLines = tooltipLines;
    }

    public RecipeData getRecipe() { return recipe; }
    /** 原始 GuiGraphics；onTooltip 里为 null */
    public GuiGraphics getGraphics() { return graphics; }
    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }

    /** 鼠标是否在矩形内 */
    public boolean isHovered(int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** 当前时间（刻），用来做动画 */
    public long getTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    // ---- 绘制（只在 onDraw 里有效） ----

    public DrawContext text(int x, int y, Component text) {
        return text(x, y, text, CategoryDef.DEFAULT_TEXT_COLOR, false);
    }

    public DrawContext text(int x, int y, Component text, int color) {
        return text(x, y, text, color, false);
    }

    public DrawContext text(int x, int y, Component text, int color, boolean shadow) {
        if (graphics != null) {
            graphics.drawString(Minecraft.getInstance().font, text, x, y, CategoryDef.withAlpha(color), shadow);
        }
        return this;
    }

    /** 实心矩形，颜色为 0xAARRGGBB（不写透明度时为不透明） */
    public DrawContext fill(int x, int y, int w, int h, int color) {
        if (graphics != null) {
            graphics.fill(x, y, x + w, y + h, CategoryDef.withAlpha(color));
        }
        return this;
    }

    /** 画物品图标（只显示，不能点击查询；需要查询请用槽位） */
    public DrawContext item(ItemStack stack, int x, int y) {
        if (graphics != null && stack != null && !stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
        }
        return this;
    }

    public DrawContext texture(ResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        return texture(texture, x, y, u, v, w, h, 256, 256);
    }

    public DrawContext texture(ResourceLocation texture, int x, int y, int u, int v, int w, int h, int texW, int texH) {
        if (graphics != null) {
            graphics.blit(texture, x, y, u, v, w, h, texW, texH);
        }
        return this;
    }

    // ---- 提示（只在 onTooltip 里有效） ----

    public DrawContext tooltip(Component line) {
        if (tooltipLines != null) {
            tooltipLines.add(line);
        }
        return this;
    }

    public static List<Component> newTooltip() {
        return new ArrayList<>();
    }
}
