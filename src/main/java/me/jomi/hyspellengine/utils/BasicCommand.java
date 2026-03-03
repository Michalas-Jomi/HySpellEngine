package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public abstract class BasicCommand extends CommandBase {
    protected final String locationPref;
    public BasicCommand(@NonNullDecl String name, @NonNullDecl String description, @NonNullDecl String location, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        if (!"".equals(location) && !location.endsWith("."))
            location += ".";
        this.locationPref = "server." + location;
    }
    public BasicCommand(@NonNullDecl String name, @NonNullDecl String description, @NonNullDecl String location) {
        this(name, description, location, false);
    }
    public BasicCommand(@NonNullDecl String name, @NonNullDecl String description) {
        this(name, description, "");
    }

        @Override
    protected final void executeSync(@NonNullDecl CommandContext context) {
        try {
            this.exe(context);
        } catch (MSG e) {
            context.sendMessage(e.location.startsWith("raw: ") ? Message.raw(e.location.substring(5)) : e.applyParams(Message.translation(this.locationPref + e.location)));
        }
    }

    protected abstract void exe(@NonNullDecl CommandContext context) throws MSG;

    protected MSG msg(String location) throws MSG {
        return new MSG(location);
    }
}
