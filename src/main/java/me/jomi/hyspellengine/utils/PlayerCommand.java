package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public abstract class PlayerCommand extends AbstractPlayerCommand {
    protected final String locationPref;
    public PlayerCommand(@NonNullDecl String name, @NonNullDecl String description, @NonNullDecl String location, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        if (!"".equals(location) && !location.endsWith("."))
            location += ".";
        this.locationPref = "server." + location;
    }
    public PlayerCommand(@NonNullDecl String name, @NonNullDecl String description, @NonNullDecl String location) {
        this(name, description, location, false);
    }
    public PlayerCommand(@NonNullDecl String name, @NonNullDecl String description) {
        this(name, description, "", false);
    }

    @Override
    protected final void execute(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef player, @NonNullDecl World world) {
        try {
            this.exe(context, store, ref, player, world);
        } catch (MSG e) {
            context.sendMessage(e.location.startsWith("raw: ") ? Message.raw(e.location.substring(5)) : e.applyParams(Message.translation(this.locationPref + e.location)));
        }
    }

    protected abstract void exe(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef player, @NonNullDecl World world) throws MSG;

    protected MSG msg(String location) throws MSG {
        return new MSG(location);
    }
}
