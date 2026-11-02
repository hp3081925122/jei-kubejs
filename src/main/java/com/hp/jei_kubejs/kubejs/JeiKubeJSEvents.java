package com.hp.jei_kubejs.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/** 脚本中的全局事件组：JEIKubeJSEvents.registerCategories / registerRecipes（client_scripts）。 */
public interface JeiKubeJSEvents {
    EventGroup GROUP = EventGroup.of("JEIKubeJSEvents");

    EventHandler REGISTER_CATEGORIES = GROUP.client("registerCategories", () -> RegisterCategoriesKubeEvent.class);
    EventHandler REGISTER_RECIPES = GROUP.client("registerRecipes", () -> RegisterRecipesKubeEvent.class);
}
