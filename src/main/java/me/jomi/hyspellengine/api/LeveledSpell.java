package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

public abstract class LeveledSpell extends Spell {
    protected final SpellField<Integer> maxLevelField;
    public LeveledSpell(String name, String description) {
        super(name, description);
        this.maxLevelField = this.requireFieldInt("max level");
    }

    /**
     * @param context spell representation in gui
     * @param ref     player reference
     * @param store   player's world store
     * @param level   level of skill, level is incremented from 1 every time when player learn next level of this skill, to check previous level use getLevel() inside
     *                
     * @see Spell#apply(SpellContext, Ref, Store)
     */
    public abstract void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level);

    @Override
    public final void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        int level = this.getLevel(context, ref, store);
        this.setLevel(context, ref, store,  level + 1);
    }

    /**
     * get player level of skill
     *
     * @param context spell context in gui
     * @param ref player reference
     * @param store player's world store
     * @return 0 if not learned, overrise player skill level
     */
    public int getLevel(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (this.has(context, ref, store) && this.getExtra(context, ref, store, "level") != null)
            return this.getExtraInt(context, ref, store, "level");
        return 0;
    }

    @Override
    public boolean canApply(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store) {
        int lvl = this.getLevel(spellContext, ref, store);
        int max = this.maxLevelField.getValue(spellContext);
        return lvl < max;
    }

    /**
     * set player level of skill
     *
     * @param context spell context in gui
     * @param ref player reference
     * @param store player's world store
     * @param level level to set
     *              
     * @see LeveledSpell#getLevel(SpellContext, Ref, Store)
     */
    public void setLevel(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level) {
        HySpellEnginePlugin.debugLog("setting lvl " + level + " max: " + maxLevelField.getValue(context));

        if (level == this.getLevel(context, ref, store))
            return;

        this.apply(context, ref, store, level);
        this.setExtra(context, ref, store, "level", level);
    }
}
