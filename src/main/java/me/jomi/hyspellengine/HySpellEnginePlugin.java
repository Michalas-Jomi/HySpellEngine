package me.jomi.hyspellengine;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.BlockGroup;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.commands.SpellsAdminCommand;
import me.jomi.hyspellengine.commands.SpellsCommand;
import me.jomi.hyspellengine.commands.SpellsExpCommand;
import me.jomi.hyspellengine.core.ExperienceRegistry;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellRegistry;
import me.jomi.hyspellengine.listeners.*;
import me.jomi.hyspellengine.spells.*;
import me.jomi.hyspellengine.utils.PlayerPacketTracker;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * HySpellEngine - A Hytale server plugin.
 *
 * @author Jomi
 * @version 1.0.0
 */
public class HySpellEnginePlugin extends JavaPlugin {
    public static class Experiences {
        public static final Experience combat = new Experience(
                "Combat",
                //"NPC Role name\nPlayer for players\n* for everything",
                "No values, just clearly damage dealt",
                str -> {
            if ("*".equals(str))
                return true;
            if ("Player".equals(str))
                return true;
            return NPCPlugin.get().getIndex(str) != Integer.MIN_VALUE;
        });
        public static final Experience mining = new Experience(
                "Mining",
                "block id like: Ore_Cobalt_Slate\n#block group name like: #Stone\n* for everything",
                str -> {
                    if ("*".equals(str))
                        return true;
                    if (null != AssetRegistry.getAssetStore(BlockGroup.class).getAssetMap().getAsset(str))
                        return true;
                    return null != AssetRegistry.getAssetStore(BlockType.class).getAssetMap().getAsset(str);
                }
        );
        public static final Experience farming = new Experience("Farming",
                "same syntax as Mining experience\nblock id\n#block group",
                str -> {
                    if ("*".equals(str))
                        return true;
                    if (null != AssetRegistry.getAssetStore(BlockType.class).getAssetMap().getAsset(str))
                        return true;
                    return null != AssetRegistry.getAssetStore(BlockGroup.class).getAssetMap().getAsset(str);
                }
        );
        public static final Experience moving = new Experience(
                "Moving",
                 "running / jumping / sprinting / walking / swimming",
                Set.of("running", "jumping", "sprinting", "walking", "swimming")::contains
        );
        public static final Experience dying = new Experience(
                "Dying",
                "values here will be ignored\n1 death = 1 exp",
                _ -> true
        );
        public static final Experience any = new Experience(
                "All",
                "other experience name : multiplier\nNo values means all x1",
                HySpellEnginePlugin.getInstance().getExperienceRegistry().getKeys()::contains
        );
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static HySpellEnginePlugin instance;
    private final SpellRegistry spellRegistry;
    private final ExperienceRegistry experienceRegistry;

    public HySpellEnginePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.spellRegistry = new SpellRegistry();
        this.experienceRegistry = new ExperienceRegistry();
    }

    public static HySpellEnginePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        //PlayerPacketTracker.registerPacketCounters();
        PlayerPacketTracker.registerStreams();

        this.registerComponents();
        this.registerCommands();
        this.registerListeners();

        this.registerExperiences();
        this.registerSpells();
    }

    private void registerExperiences() {
        this.getExperienceRegistry().registerExperience(Experiences.combat);
        this.getExperienceRegistry().registerExperience(Experiences.mining);
        this.getExperienceRegistry().registerExperience(Experiences.moving);
        this.getExperienceRegistry().registerExperience(Experiences.farming);
        this.getExperienceRegistry().registerExperience(Experiences.dying);
        this.getExperienceRegistry().registerExperience(Experiences.any);
    }

    private void registerSpells() {
        this.getSpellRegistry().registerSpell(new PermissionSpell());
        this.getSpellRegistry().registerSpell(new StatsSpell());
        this.getSpellRegistry().registerSpell(new ParticleSpell());
        this.getSpellRegistry().registerSpell(new TeleportSpell());
        this.getSpellRegistry().registerSpell(new EqSpell());
        this.getSpellRegistry().registerSpell(new CommandSpell());
    }

    private void registerListeners() {
        this.getEntityStoreRegistry().registerSystem(new ParticleSpellTickingSystem());
        this.getEntityStoreRegistry().registerSystem(new EntityDamageSystem());
        this.getEntityStoreRegistry().registerSystem(new EntityDeathSystem());
        this.getEntityStoreRegistry().registerSystem(new BlockBreakSystem());
        this.getEntityStoreRegistry().registerSystem(new MovementSystem());

        this.getEntityStoreRegistry().registerSystem(new ExperienceChangeEventSystem());
        this.getEntityStoreRegistry().registerSystem(new LevelUpEventSystem());
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new SpellsCommand());
        this.getCommandRegistry().registerCommand(new SpellsAdminCommand());
        this.getCommandRegistry().registerCommand(new SpellsExpCommand());
    }

    private Map<Class<? extends Component<EntityStore>>, ComponentType<EntityStore, ? extends Component<EntityStore>>> componentTypeMap = new HashMap<>();
    private void registerComponents() {
        registerComponent(SpellContext.SpellComponent.class, SpellContext.SpellComponent.CODEC);
        registerComponent(Experience.ExperienceComponent.class, Experience.ExperienceComponent.CODEC);
        registerComponent(ParticleSpell.ParticleSpellComponent.class, ParticleSpell.ParticleSpellComponent.CODEC);
    }
    private <T extends Component<EntityStore>> void registerComponent(Class<T> clazz, BuilderCodec<T> codec) {
        ComponentType<EntityStore, T> type = getEntityStoreRegistry().registerComponent(
                clazz,
                "hyspellengine" + clazz.getSimpleName().toLowerCase(),
                codec
        );
        this.componentTypeMap.put(clazz, type);
    }
    public <T extends Component<EntityStore>> ComponentType<EntityStore, T> getComponentType(Class<T> clazz) {
        return (ComponentType<EntityStore, T>) componentTypeMap.get(clazz);
    }

    @Override
    protected void start() {
        Data.load();
    }

    @Override
    protected void shutdown() {
    }

    public SpellRegistry getSpellRegistry() {
        return this.spellRegistry;
    }
    public ExperienceRegistry getExperienceRegistry() {
        return this.experienceRegistry;
    }

    public static String arrayToString(Object[] array) {
        return "[" + String.join(", ", Arrays.stream(array).map(Object::toString).toList()) + "]";
    }
    public static void debugLog(Object msg) {
        //HySpellEnginePlugin.log("[DEBUG]: \"" + msg + "\"");
    }
    public static void log(String msg) {
        LOGGER.at(Level.INFO).log(msg);
    }
    public static void warn(String msg) {
        LOGGER.at(Level.WARNING).log(msg);
    }
    public static void error(String msg) {
        LOGGER.at(Level.SEVERE).log(msg);
    }
}