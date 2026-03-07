package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.core.SpellContext;

/**
 * Easy and ready tu use without Override</br>
 * makes spell that gives a component when learned
 *
 * <p>Example</br>
 *         Spell.getSpellRegistry().registerSpell(new FlagComponentSpell<>("Immortale", "makes player immortal", Invulnerable.getComponentType()));</br></p>
 *
 * @param <C> Component
 */
public class FlagComponentSpell<C extends Component<EntityStore>> extends ComponentSpell<C> {
    protected FlagComponentSpell(String name, String description, ComponentType<EntityStore, C> componentType) {
        super(name, description, componentType);
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, C component) {
    }
}
