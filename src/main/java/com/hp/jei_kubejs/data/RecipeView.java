package com.hp.jei_kubejs.data;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * fromRecipeType 回调里拿到的现有配方。
 * 常用信息直接给出；模组特有的数据用 recipe() 拿到原始对象后自己调用它的方法。
 * 脚本可见的方法与 1.21.1 版完全一致（id / recipe / inputs / input / output）。
 */
public class RecipeView {
    private final Recipe<?> recipe;
    private final RegistryAccess registries;

    public RecipeView(Recipe<?> recipe, RegistryAccess registries) {
        this.recipe = recipe;
        this.registries = registries;
    }

    /** 配方 id，例如 minecraft:iron_ingot_from_smelting_iron_ore */
    public ResourceLocation id() {
        return recipe.getId();
    }

    /** 原始配方对象 */
    public Recipe<?> recipe() {
        return recipe;
    }

    /** 全部输入 */
    public List<Ingredient> inputs() {
        return recipe.getIngredients();
    }

    /** 第 index 个输入，超出范围返回空 */
    public Ingredient input(int index) {
        List<Ingredient> inputs = inputs();
        return index >= 0 && index < inputs.size() ? inputs.get(index) : Ingredient.EMPTY;
    }

    /** 输出物品（带数量）；配方没有固定输出时为空 */
    public ItemStack output() {
        try {
            return recipe.getResultItem(registries);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
