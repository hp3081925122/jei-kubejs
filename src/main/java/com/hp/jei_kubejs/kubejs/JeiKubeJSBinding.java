package com.hp.jei_kubejs.kubejs;

import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.sync.RecipeSyncRequest;
import net.minecraft.resources.ResourceLocation;

public class JeiKubeJSBinding {
    public void syncRecipeTypes(String... types) {
        for (String type : types) {
            if (ResourceLocation.tryParse(type) == null) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] syncRecipeTypes：{} 不是合法的配方类型 ID，已忽略", type);
                continue;
            }
            RecipeSyncRequest.addType(type);
        }
    }

    public void syncRecipeIds(String... prefixes) {
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] syncRecipeIds：前缀不能为空，已忽略");
                continue;
            }
            RecipeSyncRequest.addPrefix(prefix);
        }
    }
}
