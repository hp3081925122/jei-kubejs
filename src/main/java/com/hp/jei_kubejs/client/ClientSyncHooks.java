package com.hp.jei_kubejs.client;

import com.hp.jei_kubejs.sync.SyncedRecipeJsons;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ClientSyncHooks {
    private ClientSyncHooks() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> SyncedRecipeJsons.clear());
    }
}
