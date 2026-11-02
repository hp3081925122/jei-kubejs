package com.hp.jei_kubejs.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * fromRecipeType 回调里拿到的现有配方。
 * 常用信息直接给出；模组特有的数据用 recipe() 拿到原始对象后自己调用它的方法。
 */
public class RecipeView {
    private final RecipeHolder<?> holder;
    private final HolderLookup.Provider registries;

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
}
