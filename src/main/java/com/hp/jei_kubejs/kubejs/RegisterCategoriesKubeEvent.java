package com.hp.jei_kubejs.kubejs;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.data.CategoryDef;
import dev.latvian.mods.kubejs.client.ClientKubeEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RegisterCategoriesKubeEvent implements ClientKubeEvent {
    private final Map<ResourceLocation, CategoryDef> categories = new LinkedHashMap<>();

    /** event.create('mypack:altar', cat => cat.title(...).size(150, 60)...) */
    public CategoryDef create(ResourceLocation id, Consumer<CategoryDef> builder) {
        if (categories.containsKey(id)) {
            JeiKubejs.LOGGER.warn("[JEI KubeJS] 分类 {} 重复定义，后一次覆盖前一次", id);
        }
        CategoryDef def = new CategoryDef(id);
        builder.accept(def);
        categories.put(id, def);
        return def;
    }

    public Map<ResourceLocation, CategoryDef> getCategories() {
        return categories;
    }
}
