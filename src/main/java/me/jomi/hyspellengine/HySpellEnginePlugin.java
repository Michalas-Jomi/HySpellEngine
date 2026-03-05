package me.jomi.hyspellengine;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.commands.SpellsCommand;
import me.jomi.hyspellengine.core.ExperienceRegistry;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellRegistry;
import me.jomi.hyspellengine.spells.PermissionSpell;
import me.jomi.hyspellengine.spells.StatsSpell;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * HySpellEngine - A Hytale server plugin.
 *
 * @author Jomi
 * @version 1.0.0
 */
public class HySpellEnginePlugin extends JavaPlugin {
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
        this.registerComponents();
        this.registerCommands();

        this.registerExperiences();
        this.registerSpells();
    }

    private void registerExperiences() {
        this.getExperienceRegistry().registerExperience(new Experience("Combat"));
        this.getExperienceRegistry().registerExperience(new Experience("Farming"));
        this.getExperienceRegistry().registerExperience(new Experience("Mining"));
        this.getExperienceRegistry().registerExperience(new Experience("Moving"));
    }

    private void registerSpells() {
        this.getSpellRegistry().registerSpell(new PermissionSpell());
        this.getSpellRegistry().registerSpell(new StatsSpell());
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new SpellsCommand());
    }

    private Map<Class<? extends Component<EntityStore>>, ComponentType<EntityStore, ? extends Component<EntityStore>>> componentTypeMap = new HashMap<>();
    private void registerComponents() {
        registerComponent(SpellContext.SpellComponent.class, SpellContext.SpellComponent.CODEC);
        registerComponent(Experience.ExperienceComponent.class, Experience.ExperienceComponent.CODEC);
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
    }

    @Override
    protected void shutdown() {
        instance = null;
    }

    public SpellRegistry getSpellRegistry() {
        return this.spellRegistry;
    }
    public ExperienceRegistry getExperienceRegistry() {
        return this.experienceRegistry;
    }

    public static void debugLog(String msg) {
        HySpellEnginePlugin.log(msg);
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