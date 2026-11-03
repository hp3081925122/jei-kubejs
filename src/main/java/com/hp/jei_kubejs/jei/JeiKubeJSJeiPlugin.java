package com.hp.jei_kubejs.jei;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.CategoryDef;
import com.hp.jei_kubejs.data.CategoryValidator;
import com.hp.jei_kubejs.data.ClickAreaDef;
import com.hp.jei_kubejs.data.RecipeData;
import com.hp.jei_kubejs.kubejs.JeiKubeJSEvents;
import com.hp.jei_kubejs.kubejs.RegisterCategoriesKubeEvent;
import com.hp.jei_kubejs.kubejs.RegisterRecipesKubeEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 每次（重新）注册时触发脚本事件，把脚本定义的分类、配方、催化剂注册给 JEI。
 * 事件顺序：registerCategories -> registerRecipes -> registerRecipeCatalysts。
 */
@JeiPlugin
public class JeiKubeJSJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = JeiKubejs.id("jei_plugin");

    private Map<ResourceLocation, ScriptRecipeCategory> categories = Map.of();
    private RegisterRecipesKubeEvent deferredEvent;
    private IJeiRuntime pendingRuntime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        RegisterCategoriesKubeEvent event = new RegisterCategoriesKubeEvent();
        try {
            if (JeiKubeJSEvents.REGISTER_CATEGORIES.hasListeners()) {
                JeiKubeJSEvents.REGISTER_CATEGORIES.post(ScriptType.CLIENT, event);
            }
        } catch (Exception e) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] registerCategories 脚本出错", e);
        }

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        Map<ResourceLocation, ScriptRecipeCategory> built = new LinkedHashMap<>();
        for (CategoryDef def : event.getCategories().values()) {
            try {
                CategoryValidator.validate(def);
                built.put(def.getId(), new ScriptRecipeCategory(def, guiHelper));
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 分类 {} 创建失败，已跳过", def.getId(), e);
            }
        }
        categories = built;
        if (!built.isEmpty()) {
            registration.addRecipeCategories(built.values().toArray(ScriptRecipeCategory[]::new));
        }
        JeiKubejs.LOGGER.info("[JEI KubeJS] 注册了 {} 个分类: {}", built.size(), built.keySet());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Map<ResourceLocation, CategoryDef> defs = new LinkedHashMap<>();
        categories.forEach((id, category) -> defs.put(id, category.getDef()));

        RegisterRecipesKubeEvent event = new RegisterRecipesKubeEvent(defs);
        try {
            if (JeiKubeJSEvents.REGISTER_RECIPES.hasListeners()) {
                JeiKubeJSEvents.REGISTER_RECIPES.post(ScriptType.CLIENT, event);
            }
        } catch (Exception e) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] registerRecipes 脚本出错", e);
        }

        groupByCategory(event.getRecipes()).forEach((id, list) -> registration.addRecipes(categories.get(id).getRecipeType(), list));
        JeiKubejs.LOGGER.info("[JEI KubeJS] 添加了 {} 条配方", event.getRecipes().size());
        deferredEvent = event.hasDeferred() ? event : null;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RegisterRecipesKubeEvent event = deferredEvent;
        deferredEvent = null;
        if (event == null) {
            return;
        }
        pendingRuntime = runtime;
        Minecraft.getInstance().tell(() -> {
            if (pendingRuntime != runtime) {
                return;
            }
            pendingRuntime = null;
            IRecipeManager manager = runtime.getRecipeManager();
            List<RecipeData> recipes = event.runDeferred(manager, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
            groupByCategory(recipes).forEach((id, list) -> manager.addRecipes(categories.get(id).getRecipeType(), list));
            JeiKubejs.LOGGER.info("[JEI KubeJS] 运行时从 JEI 分类添加了 {} 条配方", recipes.size());
        });
    }

    @Override
    public void onRuntimeUnavailable() {
        deferredEvent = null;
        pendingRuntime = null;
    }

    private static Map<ResourceLocation, List<RecipeData>> groupByCategory(List<RecipeData> recipes) {
        Map<ResourceLocation, List<RecipeData>> byCategory = new LinkedHashMap<>();
        for (RecipeData recipe : recipes) {
            byCategory.computeIfAbsent(recipe.getCategory().getId(), k -> new ArrayList<>()).add(recipe);
        }
        return byCategory;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ScriptRecipeCategory category : categories.values()) {
            for (ItemStack stack : category.getDef().getCatalysts()) {
                if (!stack.isEmpty()) {
                    registration.addRecipeCatalyst(stack, category.getRecipeType());
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        for (ScriptRecipeCategory category : categories.values()) {
            for (ClickAreaDef area : category.getDef().getClickAreas()) {
                try {
                    Class<?> cls = Class.forName(area.screenClass(), false, JeiKubeJSJeiPlugin.class.getClassLoader());
                    if (!AbstractContainerScreen.class.isAssignableFrom(cls)) {
                        JeiKubejs.LOGGER.error("[JEI KubeJS] {} 不是容器界面类，点击区已跳过", area.screenClass());
                        continue;
                    }
                    registration.addRecipeClickArea((Class<? extends AbstractContainerScreen<?>>) cls,
                            area.x(), area.y(), area.width(), area.height(), category.getRecipeType());
                } catch (ClassNotFoundException e) {
                    JeiKubejs.LOGGER.error("[JEI KubeJS] 找不到界面类 {}，点击区已跳过", area.screenClass());
                }
            }
        }
    }
}
