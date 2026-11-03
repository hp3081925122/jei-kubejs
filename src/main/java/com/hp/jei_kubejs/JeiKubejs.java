package com.hp.jei_kubejs;

import com.mojang.logging.LogUtils;
import com.hp.jei_kubejs.client.ClientSyncHooks;
import com.hp.jei_kubejs.sync.RecipeSyncNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JeiKubejs.MOD_ID)
public final class JeiKubejs {
    public static final String MOD_ID = "jei_kubejs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JeiKubejs() {
        RecipeSyncNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(RecipeSyncNetwork::onDatapackSync);
        if (FMLEnvironment.dist.isClient()) {
            ClientSyncHooks.register();
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
