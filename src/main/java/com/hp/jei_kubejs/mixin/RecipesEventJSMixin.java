package com.hp.jei_kubejs.mixin;

import com.google.gson.JsonElement;
import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.sync.RecipeJsonCapture;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = RecipesEventJS.class, remap = false)
public abstract class RecipesEventJSMixin {
    @Inject(method = "post", at = @At("HEAD"), require = 0)
    private void jeiKubejs$begin(RecipeManager recipeManager, Map<ResourceLocation, JsonElement> map, CallbackInfo ci) {
        RecipeJsonCapture.begin(map);
    }

    @Inject(method = "post", at = @At("RETURN"), require = 0)
    private void jeiKubejs$overlay(RecipeManager recipeManager, Map<ResourceLocation, JsonElement> map, CallbackInfo ci) {
        RecipesEventJS self = (RecipesEventJS) (Object) this;
        for (RecipeJS recipe : self.originalRecipes.values()) {
            jeiKubejs$put(recipe);
        }
        for (RecipeJS recipe : self.addedRecipes) {
            jeiKubejs$put(recipe);
        }
    }

    private static void jeiKubejs$put(RecipeJS recipe) {
        if (recipe.removed || recipe.json == null) {
            return;
        }
        try {
            RecipeJsonCapture.put(recipe.getOrCreateId(), recipe.json);
        } catch (Exception e) {
            JeiKubejs.LOGGER.debug("[JEI KubeJS] 记录 KubeJS 配方 JSON 失败", e);
        }
    }
}
