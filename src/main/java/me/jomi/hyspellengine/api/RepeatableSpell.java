package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.core.SpellContext;

public abstract class RepeatableSpell extends Spell {
    public RepeatableSpell(String name, String description) {
        super(name, description);
    }

    @Override
    public final void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
    }
}
