package com.hp.jei_kubejs.sync;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class RecipeJsonCapture {
    private static Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
    private static int generation;

    private RecipeJsonCapture() {
    }

    public static synchronized void begin(Map<ResourceLocation, JsonElement> map) {
        jsons = new HashMap<>(map);
        generation++;
    }

    public static synchronized void put(ResourceLocation id, JsonElement json) {
        jsons.put(id, json);
        generation++;
    }

    public static synchronized JsonElement get(ResourceLocation id) {
        return jsons.get(id);
    }

    public static synchronized int generation() {
        return generation;
    }
}
