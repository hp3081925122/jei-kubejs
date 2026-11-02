package com.hp.jei_kubejs.kubejs;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.CategoryDef;
import com.hp.jei_kubejs.data.RecipeData;
import com.hp.jei_kubejs.data.RecipeMapper;
import com.hp.jei_kubejs.data.RecipeView;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RegisterRecipesKubeEvent extends EventJS {
    private final Map<ResourceLocation, CategoryDef> categories;
    private final List<RecipeData> recipes = new ArrayList<>();

    public RegisterRecipesKubeEvent(Map<ResourceLocation, CategoryDef> categories) {
        this.categories = categories;
    }

    /** event.add('mypack:altar', r => r.slot('in', 'minecraft:diamond')...) */
    public void add(ResourceLocation category, Consumer<RecipeData> builder) {
        CategoryDef def = getCategory(category);
        if (def == null) {
            return;
        }
        RecipeData recipe = new RecipeData(def);
        builder.accept(recipe);
        if (!recipe.isSkipped()) {
            recipes.add(recipe);
        }
    }

    /**
     * 把某个配方类型的全部配方显示到分类里，每条配方调用一次回调：
     * event.fromRecipeType('mypack:altar', 'minecraft:smelting', (recipe, r) => {
     *   r.slot('in', recipe.input(0)).stack('out', recipe.output())
     * })
     * 回调里调用 r.skip() 可以跳过某条配方。返回添加的配方数。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public int fromRecipeType(ResourceLocation category, ResourceLocation recipeType, RecipeMapper mapper) {
        CategoryDef def = getCategory(category);
        if (def == null) {
            return 0;
        }
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(recipeType);
        if (type == null) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 配方类型 {} 不存在，已跳过", recipeType);
            return 0;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 还没有进入世界，无法读取配方类型 {}", recipeType);
            return 0;
        }

        List<Recipe<?>> found = (List) level.getRecipeManager().getAllRecipesFor((RecipeType) type);
        int added = 0;
        for (Recipe<?> r : found) {
            RecipeData recipe = new RecipeData(def);
            try {
                mapper.map(new RecipeView(r, level.registryAccess()), recipe);
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 处理配方 {} 时脚本出错，已跳过", r.getId(), e);
                continue;
            }
            if (!recipe.isSkipped()) {
                recipes.add(recipe);
                added++;
            }
        }
        JeiKubejs.LOGGER.info("[JEI KubeJS] 从 {} 读取 {} 条配方，加入分类 {} 共 {} 条", recipeType, found.size(), category, added);
        return added;
    }

    private CategoryDef getCategory(ResourceLocation category) {
        CategoryDef def = categories.get(category);
        if (def == null) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 配方指向不存在的分类 {}，已跳过", category);
        }
        return def;
    }

    public List<RecipeData> getRecipes() {
        return recipes;
    }
}
