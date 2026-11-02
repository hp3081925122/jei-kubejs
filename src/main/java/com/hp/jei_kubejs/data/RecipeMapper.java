package com.hp.jei_kubejs.data;

/** fromRecipeType 的回调：(recipe, r) => { r.slot(...) } */
@FunctionalInterface
public interface RecipeMapper {
    void map(RecipeView recipe, RecipeData r);
}
