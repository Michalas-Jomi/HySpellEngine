package me.jomi.hyspellengine.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.utils.BasicCommand;
import me.jomi.hyspellengine.utils.MSG;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SpellsExpCommand extends AbstractCommandCollection {
    public static interface CommandBody {
        public void exe(Ref<EntityStore> ref, Store<EntityStore> store, Experience experience, int amount) throws MSG;
    }
    public static class Type extends AbstractCommandCollection {
        public Type(@NonNullDecl String name, CommandBody set, CommandBody give) {
            super(name, "modify player " + name);

            this.addSubCommand(new Method("set", name, true, set));
            this.addSubCommand(new Method("give", name, true, give));
            this.addSubCommand(new Method("reset", name, false, set));
            this.addSubCommand(new Method("show", name, false, (ref, store, experience, _) -> {
                throw msg(name + ".show", experience, ref, store);
            }));
        }
    }
    public static class Method extends BasicCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;
        private final RequiredArg<String> experienceArg;
        private final CommandBody body;
        private final boolean hasAmount;

        public Method(String name, String type, boolean hasAmount, CommandBody body) {
            super(name, name + " player " + type);
            this.requirePermission(HytalePermissions.fromCommand("spellsexp", name));

            this.experienceArg = this.withRequiredArg("experience", "experience to modify", ArgTypes.STRING);
            this.playerArg = this.withRequiredArg("player", "player name", ArgTypes.PLAYER_REF);
            this.amountArg = hasAmount ? this.withRequiredArg("amount", "amount to give", ArgTypes.INTEGER) : null;
            this.hasAmount = hasAmount;
            this.body = body;
        }

        @Override
        protected void exe(@NonNullDecl CommandContext context) throws MSG {
            PlayerRef player = this.playerArg.get(context);
            int amount = this.hasAmount ? this.amountArg.get(context) : 0;
            String expName = this.experienceArg.get(context);

            Experience experience = HySpellEnginePlugin.getInstance().getExperienceRegistry().getExperience(expName);
            if (experience == null)
                throw SpellsExpCommand.msg("experience.exists").p("experience", expName).p("all", String.join(", ", HySpellEnginePlugin.getInstance().getExperienceRegistry().getKeys()));

            World world = Universe.get().getWorld(player.getWorldUuid());
            if (world.isInThread())
                body.exe(player.getReference(), player.getReference().getStore(), experience, amount);
            else
                world.execute(() -> {
                    try {
                        body.exe(player.getReference(), player.getReference().getStore(), experience, amount);
                    } catch (MSG msg) {
                        context.sendMessage(msg.make(""));
                    }
                });
        }
    }

    // spellsexp exp/levels give/set <player> <amount>
    public SpellsExpCommand() {
        super("spellsexp", "manipulate spells experience and points");
        this.addAliases("spellsexperience");
        this.requirePermission(HytalePermissions.fromCommand("spellsexp"));

        this.addSubCommand(new Type(
                "exp",
                (ref, store, experience, amount) -> {
                    if (amount < 0)
                        throw msg("exp.set.min");
                    experience.setExp(ref, store, amount);
                    throw msg("exp.set.done", experience, ref, store);
                },
                (ref, store, experience, amount) -> {
                    if (experience.getExp(ref, store) + amount < 0)
                        throw msg("exp.give.min");
                    experience.addExp(ref, store, amount);
                    throw msg("exp.give.done", experience, ref, store).param("give", "" + amount);
                }));
        this.addSubCommand(new Type(
                "levels",
                (ref, store, experience, amount) -> {
                    if (amount < 0)
                        throw msg("levels.set.min");
                    double exp;
                    try {
                        exp = experience.getExpForLevel(amount);
                    } catch (IllegalArgumentException e) {
                        throw msg("levels.set.max")
                                .param("max", "" + experience.getMaxLevel())
                                .param("experience", experience.getName());
                    }
                    experience.setExp(ref, store, exp);
                    throw msg("levels.set.done", experience, ref, store);
                },
                (ref, store, experience, amount) -> {
                    if (amount > 0 && !experience.canReachNextLevel(ref, store))
                        throw msg("levels.give.max", experience, ref, store);
                    int lvl = experience.getLevel(ref, store);
                    if (amount < 0 && lvl == 0)
                        throw msg("levels.give.min");

                    double exp;
                    try {
                        exp = experience.getExpForLevel(lvl + amount);
                    } catch (IllegalArgumentException e) {
                        exp = experience.getExpForLevel(experience.getMaxLevel());
                    }
                    experience.setExp(ref, store, exp);
                    throw msg("levels.give.done", experience, ref, store).param("give", "" + amount);
                }));
    }

    protected static MSG msg(String loc) {
        return new MSG("server.commands.spellsexp." + loc);
    }
    protected static MSG msg(String loc, Experience experience, Ref<EntityStore> ref, Store<EntityStore> store) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        int exp = (int) experience.getExp(ref, store);
        int lvl = experience.getLevel(ref, store);
        return msg(loc)
                .param("player", player.getUsername())
                .param("experience", experience.getName())
                .param("exp", "" + exp)
                .param("lvl", "" + lvl);
    }
}
