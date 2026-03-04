package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.core.SpellContext;

public class FlagComponentSpell<C extends Component<EntityStore>> extends Spell {
    private final ComponentType<EntityStore, C> componentType;

    protected FlagComponentSpell(String name, String description, ComponentType<EntityStore, C> componentType) {
        super(name, description);
        this.componentType = componentType;
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        store.ensureComponent(ref, this.getComponentType());
    }

    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        store.removeComponentIfExists(ref, this.getComponentType());
    }

    public ComponentType<EntityStore, C> getComponentType() {
        return componentType;
    }
}
