package me.jomi.hyspellengine.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.ui.SpellsAdminUIPage;
import me.jomi.hyspellengine.utils.MSG;
import me.jomi.hyspellengine.utils.PlayerCommand;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SpellsAdminCommand extends PlayerCommand {
    public SpellsAdminCommand() {
        super("spellsedit", "Edit spells");
        this.requirePermission(HytalePermissions.fromCommand("spellsedit"));
    }

    @Override
    protected void exe(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef player, @NonNullDecl World world) throws MSG {
        Player p = store.getComponent(ref, Player.getComponentType());
        p.getPageManager().openCustomPage(ref, store, new SpellsAdminUIPage(player));
    }
}
