package com.hp.jei_kubejs.sync;

import com.hp.jei_kubejs.JeiKubejs;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class SyncedRecipeJsons {
    private static volatile Map<ResourceLocation, String> jsons;
    private static byte[][] parts;

    private SyncedRecipeJsons() {
    }

    public static synchronized void clear() {
        jsons = null;
        parts = null;
    }

    public static synchronized void accept(int part, int total, byte[] data) {
        if (total <= 0 || part < 0 || part >= total) {
            return;
        }
        if (part == 0 || parts == null || parts.length != total) {
            parts = new byte[total][];
        }
        parts[part] = data;
        for (byte[] p : parts) {
            if (p == null) {
                return;
            }
        }
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            all.writeBytes(p);
        }
        parts = null;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(all.toByteArray())))) {
            int count = in.readInt();
            Map<ResourceLocation, String> map = new HashMap<>(Math.max(16, count * 2));
            for (int i = 0; i < count; i++) {
                ResourceLocation id = ResourceLocation.tryParse(readString(in));
                String json = readString(in);
                if (id != null) {
                    map.put(id, json);
                }
            }
            jsons = map;
            JeiKubejs.LOGGER.info("[JEI KubeJS] 收到服务端同步的配方 JSON {} 条", map.size());
        } catch (Exception e) {
            JeiKubejs.LOGGER.error("[JEI KubeJS] 解析服务端同步的配方 JSON 失败", e);
        }
    }

    public static boolean isSynced() {
        return jsons != null;
    }

    public static String get(ResourceLocation id) {
        Map<ResourceLocation, String> map = jsons;
        return map == null ? null : map.get(id);
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = new byte[in.readInt()];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
