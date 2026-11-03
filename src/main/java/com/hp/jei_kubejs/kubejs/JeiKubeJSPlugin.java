package com.hp.jei_kubejs.kubejs;

import com.hp.jei_kubejs.sync.RecipeSyncRequest;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

public class JeiKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        JeiKubeJSEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType() == ScriptType.CLIENT) {
            RecipeSyncRequest.clear();
            event.add("JEIKubeJS", new JeiKubeJSBinding());
        }
    }
}
