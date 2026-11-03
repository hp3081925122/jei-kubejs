package com.hp.jei_kubejs.data;

@FunctionalInterface
public interface JeiRecipeMapper {
    void map(JeiRecipeView recipe, RecipeData r);
}
