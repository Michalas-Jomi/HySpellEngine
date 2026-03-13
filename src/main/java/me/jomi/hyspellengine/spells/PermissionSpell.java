package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

import java.util.Collections;

public class PermissionSpell extends Spell {
    private final SpellField<String> permField;
    public PermissionSpell() {
        super("Permission", "Give player permission after learned this spell");
        this.permField = this.requireFieldString("permission", "permission to gain after learning skill");
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        String perm = permField.getValue(context);
        PermissionsModule.get().addUserPermission(player.getUuid(), Collections.singleton(perm));
    }

    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        String perm = permField.getValue(context);
        PermissionsModule.get().removeUserPermission(player.getUuid(), Collections.singleton(perm));
    }
}
