package com.hp.jei_kubejs.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.hp.jei_kubejs.JeiKubejs;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * fromRecipeType 回调里拿到的现有配方。
 * 常用信息直接给出；模组特有的数据用 recipe() 拿到原始对象后自己调用它的方法。
 */
public class RecipeView {
    private final RecipeHolder<?> holder;
    private final HolderLookup.Provider registries;
    private Object json;
    private boolean jsonLoaded;

    public RecipeView(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        this.holder = holder;
        this.registries = registries;
    }

    /** 配方 id，例如 minecraft:iron_ingot_from_smelting_iron_ore */
    public ResourceLocation id() {
        return holder.id();
    }

    /** 原始配方对象 */
    public Recipe<?> recipe() {
        return holder.value();
    }

    /** 全部输入 */
    public List<Ingredient> inputs() {
        return holder.value().getIngredients();
    }

    /** 第 index 个输入，超出范围返回空 */
    public Ingredient input(int index) {
        List<Ingredient> inputs = inputs();
        return index >= 0 && index < inputs.size() ? inputs.get(index) : Ingredient.EMPTY;
    }

    /** 输出物品（带数量）；配方没有固定输出时为空 */
    public ItemStack output() {
        try {
            return holder.value().getResultItem(registries);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public Object json() {
        if (!jsonLoaded) {
            jsonLoaded = true;
            try {
                json = Recipe.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), holder.value())
                        .resultOrPartial(error -> JeiKubejs.LOGGER.error("[JEI KubeJS] 配方 {} 无法转成 JSON：{}", holder.id(), error))
                        .map(RecipeView::toJava)
                        .orElse(null);
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 配方 {} 无法转成 JSON", holder.id(), e);
            }
        }
        return json;
    }

    private static Object toJava(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toJava(entry.getValue()));
            }
            return map;
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                list.add(toJava(child));
            }
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsDouble();
        }
        return primitive.getAsString();
    }
}
