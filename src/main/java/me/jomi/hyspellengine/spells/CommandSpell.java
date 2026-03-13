package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.backend.HytaleConsole;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

public class CommandSpell extends Spell {
    private final SpellField<Boolean> repeatableField;
    private final SpellField<String> cmdField;

    public CommandSpell() {
        super("command", "dispatch command with console with {player} placeholder as player name");
        this.repeatableField = this.requireFieldBoolean("repeatable");
        this.cmdField = this.requireFieldString("command", "Command to run as console with {player} placeholder as player name");
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        String cmd = this.cmdField.getValue(context).replace("{player}", player.getUsername());
        HytaleServer.get().getCommandManager().handleCommand(ConsoleSender.INSTANCE, cmd);
    }

    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
    }

    @Override
    public boolean canApply(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store) {
        return this.repeatableField.getValue(spellContext);
    }
}
