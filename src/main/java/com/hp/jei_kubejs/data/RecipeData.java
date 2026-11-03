package com.hp.jei_kubejs.data;

import com.hp.jei_kubejs.JeiKubejs;
import dev.latvian.mods.rhino.util.HideFromJS;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
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
    private final Map<String, List<FluidEntry>> fluids = new LinkedHashMap<>();
    private final Map<String, List<ITypedIngredient<?>>> copied = new LinkedHashMap<>();
    private final List<List<String>> links = new ArrayList<>();
    private ResourceLocation id;
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
        if (checkItemSlot(name) && content != null && !content.isEmpty()) {
            copied.remove(name);
            slotContents.put(name, Arrays.asList(content.getItems()));
        }
        return this;
    }

    /** r.stack('out', '4x minecraft:diamond')；保留数量和物品数据，适合输出槽 */
    public RecipeData stack(String name, ItemStack... stacks) {
        if (checkItemSlot(name)) {
            List<ItemStack> list = Arrays.stream(stacks).filter(s -> s != null && !s.isEmpty()).toList();
            if (!list.isEmpty()) {
                copied.remove(name);
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

    public RecipeData fluid(String name, String fluid) {
        return fluid(name, fluid, 1000);
    }

    public RecipeData fluid(String name, String fluid, long amount) {
        if (!checkSlot(name)) {
            return this;
        }
        if (!category.getSlots().get(name).isTank()) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的槽位 '{}' 不是流体槽（用 inputTank / outputTank 定义），已忽略", category.getId(), name);
            return this;
        }
        List<FluidEntry> list = new ArrayList<>();
        boolean tag = fluid.startsWith("#");
        ResourceLocation rl = ResourceLocation.tryParse(tag ? fluid.substring(1) : fluid);
        if (rl != null && tag) {
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(TagKey.create(Registries.FLUID, rl))) {
                Fluid f = holder.value();
                if (f.isSource(f.defaultFluidState())) {
                    list.add(new FluidEntry(f, amount));
                }
            }
        } else if (rl != null && BuiltInRegistries.FLUID.containsKey(rl)) {
            list.add(new FluidEntry(BuiltInRegistries.FLUID.get(rl), amount));
        }
        list.removeIf(e -> e.fluid() == Fluids.EMPTY);
        if (list.isEmpty()) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的流体槽 '{}'：找不到流体 {}，已忽略", category.getId(), name, fluid);
            return this;
        }
        copied.remove(name);
        fluids.put(name, list);
        return this;
    }

    public RecipeData copy(String name, JeiSlotView view) {
        if (checkSlot(name) && view != null && !view.isEmpty()) {
            slotContents.remove(name);
            fluids.remove(name);
            copied.put(name, view.getTyped());
        }
        return this;
    }

    public RecipeData id(ResourceLocation id) {
        this.id = id;
        return this;
    }

    public RecipeData linkSlots(String... names) {
        for (String name : names) {
            if (!checkSlot(name)) {
                return this;
            }
        }
        if (names.length >= 2) {
            links.add(List.of(names));
        }
        return this;
    }

    @HideFromJS
    public void finish(ResourceLocation defaultId) {
        if (id == null) {
            id = defaultId;
        }
        links.removeIf(group -> {
            List<Integer> counts = group.stream().map(this::countOf).distinct().toList();
            if (counts.size() == 1 && counts.get(0) > 0) {
                return false;
            }
            JeiKubejs.LOGGER.error("[JEI KubeJS] 配方 {} 的联动槽位 {} 内容数量不一致或为空 {}，已取消联动", id, group, group.stream().map(this::countOf).toList());
            return true;
        });
    }

    private int countOf(String name) {
        if (copied.containsKey(name)) {
            return copied.get(name).size();
        }
        List<?> list = category.getSlots().get(name).isTank() ? fluids.get(name) : slotContents.get(name);
        return list == null ? 0 : list.size();
    }

    public ResourceLocation getId() { return id; }
    public Map<String, List<FluidEntry>> getFluids() { return fluids; }
    public List<List<String>> getLinks() { return links; }
    public Map<String, List<ITypedIngredient<?>>> getCopied() { return copied; }

    public record FluidEntry(Fluid fluid, long amount) {}

    /** 在 fromRecipeType 回调里调用：这条配方不显示 */
    public RecipeData skip() {
        skipped = true;
        return this;
    }

    private boolean checkItemSlot(String name) {
        if (!checkSlot(name)) {
            return false;
        }
        if (category.getSlots().get(name).isTank()) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 的槽位 '{}' 是流体槽，请用 r.fluid 填写，已忽略", category.getId(), name);
            return false;
        }
        return true;
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
