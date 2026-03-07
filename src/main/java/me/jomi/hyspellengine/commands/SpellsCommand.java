package me.jomi.hyspellengine.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.ui.SpellsUIPage;
import me.jomi.hyspellengine.utils.MSG;
import me.jomi.hyspellengine.utils.PlayerCommand;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.nio.file.Path;
import java.util.UUID;

public class SpellsCommand extends PlayerCommand {
    public SpellsCommand() {
        super("spells", "Open Spells menu");
    }

    @Override
    protected void exe(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef player, @NonNullDecl World world) throws MSG {
        Player p = store.getComponent(ref, Player.getComponentType());
        p.getPageManager().openCustomPage(ref, store, new SpellsUIPage(player));
    }

 }
