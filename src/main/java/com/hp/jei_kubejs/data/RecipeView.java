package com.hp.jei_kubejs.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hp.jei_kubejs.JeiKubejs;
import com.hp.jei_kubejs.sync.SyncedRecipeJsons;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * fromRecipeType 回调里拿到的现有配方。
 * 常用信息直接给出；模组特有的数据用 recipe() 拿到原始对象后自己调用它的方法。
 * 脚本可见的方法与 1.21.1 版完全一致（id / recipe / inputs / input / output）。
 */
public class RecipeView {
    private final Recipe<?> recipe;
    private final RegistryAccess registries;
    private Object json;
    private boolean jsonLoaded;
    private static boolean warnedServer;
    private static boolean warnedUndeclared;
    private static boolean warnedMissing;

    public RecipeView(Recipe<?> recipe, RegistryAccess registries) {
        this.recipe = recipe;
        this.registries = registries;
    }

    /** 配方 id，例如 minecraft:iron_ingot_from_smelting_iron_ore */
    public ResourceLocation id() {
        return recipe.getId();
    }

    /** 原始配方对象 */
    public Recipe<?> recipe() {
        return recipe;
    }

    /** 全部输入 */
    public List<Ingredient> inputs() {
        return recipe.getIngredients();
    }

    /** 第 index 个输入，超出范围返回空 */
    public Ingredient input(int index) {
        List<Ingredient> inputs = inputs();
        return index >= 0 && index < inputs.size() ? inputs.get(index) : Ingredient.EMPTY;
    }

    /** 输出物品（带数量）；配方没有固定输出时为空 */
    public ItemStack output() {
        try {
            return recipe.getResultItem(registries);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public Object json() {
        if (!jsonLoaded) {
            jsonLoaded = true;
            ResourceLocation id = recipe.getId();
            String synced = SyncedRecipeJsons.get(id);
            if (synced != null) {
                try {
                    json = toJava(JsonParser.parseString(synced));
                } catch (Exception e) {
                    JeiKubejs.LOGGER.error("[JEI KubeJS] 解析配方 {} 的 JSON 失败", id, e);
                }
                return json;
            }
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                if (SyncedRecipeJsons.isSynced()) {
                    if (!warnedUndeclared) {
                        warnedUndeclared = true;
                        JeiKubejs.LOGGER.warn("[JEI KubeJS] 配方 {} 不在同步范围内（用 JEIKubeJS.syncRecipeTypes / syncRecipeIds 声明），recipe.json() 返回 null；同类情况不再重复提示", id);
                    }
                } else if (!warnedServer) {
                    warnedServer = true;
                    JeiKubejs.LOGGER.warn("[JEI KubeJS] 服务端没有安装本模组，多人游戏中 recipe.json() 返回 null");
                }
                return null;
            }
            Optional<Resource> resource = server.getResourceManager().getResource(ResourceLocation.tryBuild(id.getNamespace(), "recipes/" + id.getPath() + ".json"));
            if (resource.isEmpty()) {
                if (!warnedMissing) {
                    warnedMissing = true;
                    JeiKubejs.LOGGER.warn("[JEI KubeJS] 配方 {} 没有对应的数据包文件（例如由 KubeJS 脚本添加），1.20.1 读不到它的 JSON，recipe.json() 返回 null；同类情况不再重复提示", id);
                }
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                json = toJava(JsonParser.parseReader(reader));
            } catch (Exception e) {
                JeiKubejs.LOGGER.error("[JEI KubeJS] 读取配方 {} 的 JSON 失败", id, e);
            }
        }
        return json;
    }

    private static Object toJava(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toJava(entry.getValue()));
            }
            return map;
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                list.add(toJava(child));
            }
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsDouble();
        }
        return primitive.getAsString();
    }
}
