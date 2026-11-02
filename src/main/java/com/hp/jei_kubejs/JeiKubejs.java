package com.hp.jei_kubejs;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JeiKubejs.MOD_ID)
public final class JeiKubejs {
    public static final String MOD_ID = "jei_kubejs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JeiKubejs() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
