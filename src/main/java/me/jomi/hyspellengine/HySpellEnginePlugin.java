package me.jomi.hyspellengine;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
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

    public HySpellEnginePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static HySpellEnginePlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[HySpellEngine] Setting up...");

        // TODO: Register commands and listeners here

        LOGGER.at(Level.INFO).log("[HySpellEngine] Setup complete!");
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[HySpellEngine] Started!");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[HySpellEngine] Shutting down...");
        instance = null;
    }
}