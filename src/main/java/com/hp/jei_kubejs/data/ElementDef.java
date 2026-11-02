package com.hp.jei_kubejs.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 分类里的绘制元素，按定义顺序绘制（后定义的在上层）。
 * 这里只存数据，不依赖 JEI，方便以后给 EMI 复用。坐标都相对分类左上角。
 */
public sealed interface ElementDef {
    int x();

    int y();

    /** JEI 原生箭头。ticks > 0 时为动画箭头（一轮 ticks 刻），否则为静态箭头。 */
    record Arrow(int x, int y, int ticks) implements ElementDef {}

    /** JEI 原生火焰。ticks > 0 时为燃烧动画，否则为满火焰。 */
    record Flame(int x, int y, int ticks) implements ElementDef {}

    /** JEI 原生加号。 */
    record PlusSign(int x, int y) implements ElementDef {}

    /** 贴图的一块区域。 */
    record Texture(int x, int y, Tex tex) implements ElementDef {}

    /** 用贴图区域做的进度条动画，从 from 一侧开始填充。 */
    record Progress(int x, int y, Tex tex, int ticks, Direction from) implements ElementDef {}

    /** 固定文字，所有配方都一样。 */
    record Label(int x, int y, Component text, int color, boolean shadow) implements ElementDef {}

    /** 由配方填入内容的文字，通过 r.text(name, ...) 赋值。 */
    record Text(String name, int x, int y, int color, boolean shadow) implements ElementDef {}

    /** 固定提示区域：鼠标悬停时显示 lines。 */
    record Tooltip(int x, int y, int width, int height, List<Component> lines) implements ElementDef {}

    /** 由配方填入内容的提示区域，通过 r.tooltip(name, ...) 赋值。 */
    record TooltipArea(String name, int x, int y, int width, int height) implements ElementDef {}

    /** 可滚动的多行文字，内容由配方通过 r.scrollText(name, ...) 填入；使用 JEI 原生滚动条。 */
    record ScrollText(String name, int x, int y, int width, int height) implements ElementDef {}

    /** 可滚动面板，内容由脚本回调绘制；contentHeight 为内容总高度。 */
    record ScrollPanel(int x, int y, int width, int height, int contentHeight, DrawCallback callback) implements ElementDef {}

    /** 贴图上的一块区域。 */
    record Tex(ResourceLocation texture, int u, int v, int width, int height, int textureWidth, int textureHeight) {}

    enum Direction {
        LEFT, RIGHT, TOP, BOTTOM;

        public static Direction parse(String s) {
            return switch (s.toLowerCase()) {
                case "right" -> RIGHT;
                case "top", "up" -> TOP;
                case "bottom", "down" -> BOTTOM;
                default -> LEFT;
            };
        }
    }
}
