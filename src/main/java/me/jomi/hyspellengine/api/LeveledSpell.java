package me.jomi.hyspellengine.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

public abstract class LeveledSpell extends Spell {
    protected final SpellField<Integer> levelField;
    public LeveledSpell(String name, String description) {
        super(name, description);
        this.levelField = this.requireFieldInt("max level");
    }

    /**
     *
     * @param context
     * @param ref     player reference
     * @param store   store
     * @param level   level of skill, level is incremented from 1 every time when player learn next level of this skill, to check previous level use getLevel() inside
     */
    public abstract void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level);

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        this.setLevel(context, ref, store, 1);
    }

    public int getLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (this.has(ref, store))
            return this.getExtraInt(ref, store, "level");
        return 0;
    }

    public void setLevel(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level) {
        if (level == this.getLevel(ref, store))
            return;

        this.apply(context, ref, store, level);
        this.setExtra(ref, store, "level", level);
    }
}
