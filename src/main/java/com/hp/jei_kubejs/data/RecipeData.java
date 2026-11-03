package com.hp.jei_kubejs.data;

import com.hp.jei_kubejs.JeiKubejs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 脚本添加的一条配方：槽位、文字、提示区域的内容。 */
public class RecipeData {
    private final CategoryDef category;
    /** 槽位名 -> 轮换显示的物品（保留数量） */
    private final Map<String, List<ItemStack>> slotContents = new LinkedHashMap<>();
    private final Map<String, Component> texts = new LinkedHashMap<>();
    private final Map<String, List<Component>> tooltips = new LinkedHashMap<>();
    private final Map<String, List<Component>> scrollTexts = new LinkedHashMap<>();
    private final Map<String, List<Component>> slotTooltips = new LinkedHashMap<>();
    /** 脚本自定义数据，供 onDraw / onTooltip 回调读取 */
    private final Map<String, Object> data = new LinkedHashMap<>();
    private boolean skipped;

    public RecipeData(CategoryDef category) {
        this.category = category;
    }

    /** r.data('mana', 500)；回调里用 ctx.recipe.getData('mana') 读取 */
    public RecipeData data(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    /** r.slot('in', 'minecraft:diamond') 或 r.slot('in', '#c:gems')；不带数量 */
    public RecipeData slot(String name, Ingredient content) {
        if (checkSlot(name) && content != null && !content.isEmpty()) {
            slotContents.put(name, Arrays.asList(content.getItems()));
        }
        return this;
    }

    /** r.stack('out', '4x minecraft:diamond')；保留数量和物品数据，适合输出槽 */
    public RecipeData stack(String name, ItemStack... stacks) {
        if (checkSlot(name)) {
            List<ItemStack> list = Arrays.stream(stacks).filter(s -> s != null && !s.isEmpty()).toList();
            if (!list.isEmpty()) {
                slotContents.put(name, list);
            }
        }
        return this;
    }

    /** r.text('time', '需要 30 秒') */
    public RecipeData text(String name, Component text) {
        if (!category.hasText(name)) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 没有文字 '{}'（需先在分类里用 text 定义），已忽略", category.getId(), name);
            return this;
        }
        texts.put(name, text);
        return this;
    }

    /** r.tooltip('info', '第一行', '第二行') */
    public RecipeData tooltip(String name, Component... lines) {
        if (!category.hasTooltipArea(name)) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 没有提示区域 '{}'（需先在分类里用 tooltipArea 定义），已忽略", category.getId(), name);
            return this;
        }
        tooltips.put(name, List.of(lines));
        return this;
    }

    /** r.scrollText('desc', '第一行', '第二行', ...)：可滚动文字区的内容 */
    public RecipeData scrollText(String name, Component... lines) {
        if (!category.hasScrollText(name)) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 没有滚动文字区 '{}'（需先在分类里用 scrollText 定义），已忽略", category.getId(), name);
            return this;
        }
        scrollTexts.put(name, List.of(lines));
        return this;
    }

    public Map<String, List<Component>> getScrollTexts() { return scrollTexts; }

    public RecipeData slotTooltip(String name, Component... lines) {
        if (checkSlot(name)) {
            slotTooltips.put(name, List.of(lines));
        }
        return this;
    }

    public Map<String, List<Component>> getSlotTooltips() { return slotTooltips; }

    /** 在 fromRecipeType 回调里调用：这条配方不显示 */
    public RecipeData skip() {
        skipped = true;
        return this;
    }

    private boolean checkSlot(String name) {
        if (!category.getSlots().containsKey(name)) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 没有槽位 '{}'，已忽略", category.getId(), name);
            return false;
        }
        return true;
    }

    public CategoryDef getCategory() { return category; }
    public Map<String, List<ItemStack>> getSlotContents() { return slotContents; }
    public Map<String, Component> getTexts() { return texts; }
    public Map<String, List<Component>> getTooltips() { return tooltips; }
    public boolean isSkipped() { return skipped; }
}
