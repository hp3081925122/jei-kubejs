package com.hp.jei_kubejs.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;

public class JeiKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        JeiKubeJSEvents.GROUP.register();
    }
}
