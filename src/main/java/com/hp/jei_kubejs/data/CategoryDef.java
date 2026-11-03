package com.hp.jei_kubejs.data;

import com.hp.jei_kubejs.JeiKubejs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 脚本定义的 JEI 分类。方法返回 this，供脚本链式调用。 */
public class CategoryDef {
    /** JEI 默认的配方文字颜色（深灰） */
    public static final int DEFAULT_TEXT_COLOR = 0xFF404040;

    private final ResourceLocation id;
    private Component title;
    private ItemStack icon = ItemStack.EMPTY;
    private int width = 150;
    private int height = 60;
    private final Map<String, SlotDef> slots = new LinkedHashMap<>();
    private final List<ItemStack> catalysts = new ArrayList<>();
    private final List<ElementDef> elements = new ArrayList<>();
    private final Set<String> textNames = new HashSet<>();
    private final Set<String> tooltipNames = new HashSet<>();
    private final Set<String> scrollTextNames = new HashSet<>();
    private final List<ClickAreaDef> clickAreas = new ArrayList<>();
    private DrawCallback drawCallback;
    private DrawCallback tooltipCallback;

    public CategoryDef(ResourceLocation id) {
        this.id = id;
        this.title = Component.literal(id.toString());
    }

    // ---- 基本信息 ----

    public CategoryDef title(Component title) {
        this.title = title;
        return this;
    }

    public CategoryDef icon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public CategoryDef size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public CategoryDef catalyst(ItemStack stack) {
        catalysts.add(stack);
        return this;
    }

    /**
     * 在某个界面上加配方点击区，点击后打开本分类。
     * cat.clickArea('net.minecraft.client.gui.screens.inventory.FurnaceScreen', 78, 32, 28, 23)
     */
    public CategoryDef clickArea(String screenClass, int x, int y, int w, int h) {
        clickAreas.add(new ClickAreaDef(screenClass, x, y, w, h));
        return this;
    }

    // ---- 槽位 ----

    public CategoryDef inputSlot(String name, int x, int y) {
        return slot(name, SlotDef.Role.INPUT, x, y);
    }

    public CategoryDef outputSlot(String name, int x, int y) {
        return slot(name, SlotDef.Role.OUTPUT, x, y);
    }

    public CategoryDef renderSlot(String name, int x, int y) {
        return slot(name, SlotDef.Role.RENDER_ONLY, x, y);
    }

    public CategoryDef inputTank(String name, int x, int y, int w, int h, long capacity) {
        return tank(name, SlotDef.Role.INPUT, x, y, w, h, capacity);
    }

    public CategoryDef outputTank(String name, int x, int y, int w, int h, long capacity) {
        return tank(name, SlotDef.Role.OUTPUT, x, y, w, h, capacity);
    }

    private CategoryDef tank(String name, SlotDef.Role role, int x, int y, int w, int h, long capacity) {
        if (w <= 0 || h <= 0 || capacity <= 0) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的流体槽 '{}' 尺寸 {}x{} 或容量 {} 无效，已忽略", id, name, w, h, capacity);
            return this;
        }
        return put(new SlotDef(name, role, x, y, w, h, capacity));
    }

    private CategoryDef slot(String name, SlotDef.Role role, int x, int y) {
        return put(new SlotDef(name, role, x, y, 0, 0, 0));
    }

    private CategoryDef put(SlotDef slot) {
        if (slots.containsKey(slot.name())) {
            JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 的槽位 '{}' 重复定义，后一次覆盖前一次", id, slot.name());
        }
        slots.put(slot.name(), slot);
        return this;
    }

    // ---- 绘制元素（阶段 3） ----

    /** 静态箭头 */
    public CategoryDef arrow(int x, int y) {
        return arrow(x, y, 0);
    }

    /** 动画箭头，一轮 ticks 刻（20 刻 = 1 秒） */
    public CategoryDef arrow(int x, int y, int ticks) {
        return add(new ElementDef.Arrow(x, y, ticks));
    }

    /** 满火焰 */
    public CategoryDef flame(int x, int y) {
        return flame(x, y, 0);
    }

    /** 燃烧动画，一轮 ticks 刻 */
    public CategoryDef flame(int x, int y, int ticks) {
        return add(new ElementDef.Flame(x, y, ticks));
    }

    public CategoryDef plus(int x, int y) {
        return add(new ElementDef.PlusSign(x, y));
    }

    /** 贴图区域，按 256x256 贴图计算 UV */
    public CategoryDef texture(int x, int y, ResourceLocation texture, int u, int v, int w, int h) {
        return texture(x, y, texture, u, v, w, h, 256, 256);
    }

    public CategoryDef texture(int x, int y, ResourceLocation texture, int u, int v, int w, int h, int texW, int texH) {
        return add(new ElementDef.Texture(x, y, new ElementDef.Tex(texture, u, v, w, h, texW, texH)));
    }

    /** 进度条：从左往右填充，一轮 ticks 刻，按 256x256 贴图计算 UV */
    public CategoryDef progress(int x, int y, ResourceLocation texture, int u, int v, int w, int h, int ticks) {
        return progress(x, y, texture, u, v, w, h, ticks, "left");
    }

    /** from: 'left' | 'right' | 'top' | 'bottom'，表示从哪一侧开始填充 */
    public CategoryDef progress(int x, int y, ResourceLocation texture, int u, int v, int w, int h, int ticks, String from) {
        return progress(x, y, texture, u, v, w, h, 256, 256, ticks, from);
    }

    public CategoryDef progress(int x, int y, ResourceLocation texture, int u, int v, int w, int h, int texW, int texH, int ticks, String from) {
        return add(new ElementDef.Progress(x, y, new ElementDef.Tex(texture, u, v, w, h, texW, texH),
                Math.max(1, ticks), ElementDef.Direction.parse(from)));
    }

    /** 固定文字 */
    public CategoryDef label(int x, int y, Component text) {
        return label(x, y, text, DEFAULT_TEXT_COLOR, false);
    }

    public CategoryDef label(int x, int y, Component text, int color) {
        return label(x, y, text, color, false);
    }

    public CategoryDef label(int x, int y, Component text, int color, boolean shadow) {
        return add(new ElementDef.Label(x, y, text, withAlpha(color), shadow));
    }

    /** 由配方填入的文字：r.text(name, '...') */
    public CategoryDef text(String name, int x, int y) {
        return text(name, x, y, DEFAULT_TEXT_COLOR, false);
    }

    public CategoryDef text(String name, int x, int y, int color) {
        return text(name, x, y, color, false);
    }

    public CategoryDef text(String name, int x, int y, int color, boolean shadow) {
        textNames.add(name);
        return add(new ElementDef.Text(name, x, y, withAlpha(color), shadow));
    }

    /** 固定提示区域，可传多行 */
    public CategoryDef tooltip(int x, int y, int w, int h, Component... lines) {
        return add(new ElementDef.Tooltip(x, y, w, h, List.of(lines)));
    }

    /** 由配方填入的提示区域：r.tooltip(name, '第一行', '第二行') */
    public CategoryDef tooltipArea(String name, int x, int y, int w, int h) {
        tooltipNames.add(name);
        return add(new ElementDef.TooltipArea(name, x, y, w, h));
    }

    /** 可滚动文字区：r.scrollText(name, '第一行', '第二行', ...)，内容超出高度时出现滚动条 */
    public CategoryDef scrollText(String name, int x, int y, int w, int h) {
        scrollTextNames.add(name);
        return add(new ElementDef.ScrollText(name, x, y, w, h));
    }

    /**
     * 可滚动面板，回调里用 ctx 绘制，坐标相对内容左上角：
     * cat.scrollPanel(0, 0, 150, 60, 200, ctx => ctx.text(0, 0, '...'))
     */
    public CategoryDef scrollPanel(int x, int y, int w, int h, int contentHeight, DrawCallback callback) {
        return add(new ElementDef.ScrollPanel(x, y, w, h, Math.max(h, contentHeight), callback));
    }

    private CategoryDef add(ElementDef element) {
        elements.add(element);
        return this;
    }

    // ---- 脚本回调（阶段 5） ----

    /** 每帧在其他元素之后调用：cat.onDraw(ctx => ctx.text(0, 0, ctx.recipe.getData('name'))) */
    public CategoryDef onDraw(DrawCallback callback) {
        this.drawCallback = callback;
        return this;
    }

    /** 鼠标在分类内时调用，用 ctx.tooltip(...) 添加提示行 */
    public CategoryDef onTooltip(DrawCallback callback) {
        this.tooltipCallback = callback;
        return this;
    }

    public DrawCallback getDrawCallback() { return drawCallback; }
    public DrawCallback getTooltipCallback() { return tooltipCallback; }

    /** 脚本里写 0x404040 时没有透明度，补成不透明 */
    public static int withAlpha(int color) {
        return (color >>> 24) == 0 ? color | 0xFF000000 : color;
    }

    public ResourceLocation getId() { return id; }
    public Component getTitle() { return title; }
    public ItemStack getIcon() { return icon; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Map<String, SlotDef> getSlots() { return slots; }
    public List<ItemStack> getCatalysts() { return catalysts; }
    public List<ElementDef> getElements() { return elements; }
    public List<ClickAreaDef> getClickAreas() { return clickAreas; }
    public boolean hasText(String name) { return textNames.contains(name); }
    public boolean hasTooltipArea(String name) { return tooltipNames.contains(name); }
    public boolean hasScrollText(String name) { return scrollTextNames.contains(name); }
}
