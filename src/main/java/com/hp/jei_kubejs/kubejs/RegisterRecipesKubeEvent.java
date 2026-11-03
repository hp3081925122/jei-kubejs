package com.hp.jei_kubejs.kubejs;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.CategoryDef;
import com.hp.jei_kubejs.data.JeiRecipeMapper;
import com.hp.jei_kubejs.data.JeiRecipeView;
import com.hp.jei_kubejs.data.RecipeData;
import com.hp.jei_kubejs.data.RecipeMapper;
import com.hp.jei_kubejs.data.RecipeView;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RegisterRecipesKubeEvent extends EventJS {
    private final Map<ResourceLocation, CategoryDef> categories;
    private final List<RecipeData> recipes = new ArrayList<>();
    private final Map<ResourceLocation, Integer> counters = new HashMap<>();
    private final Set<String> usedIds = new HashSet<>();
    private final List<Deferred> deferred = new ArrayList<>();

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
        int index = counters.merge(category, 1, Integer::sum) - 1;
        accept(def, recipe, ResourceLocation.tryBuild(category.getNamespace(), category.getPath() + "/" + index));
    }

    private boolean accept(CategoryDef def, RecipeData recipe, ResourceLocation defaultId) {
        if (recipe.isSkipped()) {
            return false;
        }
        recipe.finish(defaultId);
        ResourceLocation id = recipe.getId();
        if (!usedIds.add(def.getId() + "|" + id)) {
            int n = 2;
            ResourceLocation unique;
            do {
                unique = ResourceLocation.tryBuild(id.getNamespace(), id.getPath() + "_" + n++);
            } while (!usedIds.add(def.getId() + "|" + unique));
            JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 里配方 ID {} 重复，已改为 {}", def.getId(), id, unique);
            recipe.id(unique);
        }
        recipes.add(recipe);
        return true;
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
            if (accept(def, recipe, r.getId())) {
                added++;
            }
        }
        JeiKubejs.LOGGER.info("[JEI KubeJS] 从 {} 读取 {} 条配方，加入分类 {} 共 {} 条", recipeType, found.size(), category, added);
        return added;
    }

    public void fromCategory(ResourceLocation category, ResourceLocation jeiType, JeiRecipeMapper mapper) {
        CategoryDef def = getCategory(category);
        if (def != null) {
            deferred.add(new Deferred(def, jeiType, mapper));
        }
    }

    @HideFromJS
    public boolean hasDeferred() {
        return !deferred.isEmpty();
    }

    @HideFromJS
    public List<RecipeData> runDeferred(IRecipeManager manager, IFocusGroup focus) {
        int start = recipes.size();
        for (Deferred d : deferred) {
            Optional<mezz.jei.api.recipe.RecipeType<?>> type = manager.getRecipeType(d.source());
            if (type.isEmpty()) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] JEI 分类 {} 不存在，已跳过", d.source());
                continue;
            }
            collect(manager, type.get(), d, focus);
        }
        return new ArrayList<>(recipes.subList(start, recipes.size()));
    }

    private <T> void collect(IRecipeManager manager, mezz.jei.api.recipe.RecipeType<T> type, Deferred d, IFocusGroup focus) {
        long startTime = System.nanoTime();
        IRecipeCategory<T> category = manager.getRecipeCategory(type);
        List<T> sources = manager.createRecipeLookup(type).includeHidden().get().toList();
        int added = 0;
        for (int i = 0; i < sources.size(); i++) {
            T source = sources.get(i);
            ResourceLocation sourceId = null;
            RecipeData recipe = new RecipeData(d.def());
            try {
                Optional<IRecipeLayoutDrawable<T>> layout = manager.createRecipeLayoutDrawable(category, source, focus);
                if (layout.isEmpty()) {
                    continue;
                }
                sourceId = category.getRegistryName(source);
                d.mapper().map(new JeiRecipeView(type.getUid(), sourceId, source, layout.get().getRecipeSlotsView()), recipe);
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 处理 JEI 分类 {} 的第 {} 条配方时出错，已跳过", type.getUid(), i, e);
                continue;
            }
            ResourceLocation defaultId = sourceId != null ? sourceId : ResourceLocation.tryBuild(d.def().getId().getNamespace(),
                    d.def().getId().getPath() + "/" + type.getUid().getNamespace() + "/" + type.getUid().getPath() + "/" + i);
            if (accept(d.def(), recipe, defaultId)) {
                added++;
            }
        }
        JeiKubejs.LOGGER.info("[JEI KubeJS] 从 JEI 分类 {} 读取 {} 条配方，加入分类 {} 共 {} 条，耗时 {} ms",
                type.getUid(), sources.size(), d.def().getId(), added, (System.nanoTime() - startTime) / 1_000_000);
    }

    private record Deferred(CategoryDef def, ResourceLocation source, JeiRecipeMapper mapper) {}

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
