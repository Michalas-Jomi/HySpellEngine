package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.ExperienceRegistry;

public final class Experience {
    private final String name;
    public Experience(String name) {
        this.name = name;
    }

    public int getLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        return 0; // TODO
    }

    public double getExp(Ref<EntityStore> ref, Store<EntityStore> store) {
        return 0d; // TODO
    }

    public String getName() {
        return name;
    }

    public static ExperienceRegistry getRegistry() {
        return HySpellEnginePlugin.getInstance().getExperienceRegistry();
    }
}
