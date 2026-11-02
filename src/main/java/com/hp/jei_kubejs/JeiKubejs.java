package com.hp.jei_kubejs;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JeiKubejs.MOD_ID)
public final class JeiKubejs {
    public static final String MOD_ID = "jei_kubejs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JeiKubejs(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
