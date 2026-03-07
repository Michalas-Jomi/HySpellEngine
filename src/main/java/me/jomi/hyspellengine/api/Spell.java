package me.jomi.hyspellengine.api;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;
import me.jomi.hyspellengine.core.SpellRegistry;
import org.bson.*;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spell mechanics</br>
 * note that, Spell class is Spell core, not in-gui spell</br>
 * contains data needed to make spell in admin tool</br>
 * supports learn/unlearn mechanic
 * 
 * @see SpellContext
 * @see Spell#requireField(String, Codec)
 * @see Spell#getExtra(SpellContext, Ref, Store, String) 
 */
public abstract class Spell {
    private final String name;
    private final String description;
    private final Map<String, SpellField<?>> fields = new ConcurrentHashMap<>();

    /**
     * @param name unique id for spell, visible only in admin tool
     * @param description description for admin tool
     */
    public Spell(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * apply spell mechanics for player after learned skill
     *
     * @param context spell representation in gui
     * @param ref player reference
     * @param store player's world store
     *
     * @see Spell#canApply(SpellContext, Ref, Store)
     * @see Spell#requireField
     * @see Spell#getExtra(SpellContext, Ref, Store, String) 
     */
    public abstract void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store);

    /**
     * Reverse process of apply
     * @see Spell#apply(SpellContext, Ref, Store)
     */
    public abstract void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store);

    /**
     * Optionally for Override
     *
     * @param spellContext spell representation in gui. Use SpellField.getValue(spellContext) for fields values
     * @param ref          player reference
     * @param store        player's world store
     * @param ui           ui builder
     * @param events       events builder
     * @param selector     selector in .ui ends with space. `selector + "#SpellRoot"` is a Group 88x88 contains spell button & spell icon
     */
    public void build(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, String selector) {
    }

    /**
     * Optionally for Override
     * check player can learn this spell
     *
     * @param spellContext spell representation in gui. Use SpellField.getValue(spellContext) for fields values
     * @param ref          player reference
     * @param store        player's world store
     * @return true if player can learn this skill, overrise else
     */
    public boolean canApply(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store) {
        return this.has(spellContext, ref, store);
    }

    /**
     * check player has learned spell
     *
     * @param spellContext spell representation in gui
     * @param ref          player reference
     * @param store        player's world store
     * @return true if player has learned this spell
     */
    public final boolean has(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store) {
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        return component != null && component.hasSpell(spellContext);
    }


    /**
     * Extra values are per player data
     *
     * @param context spell representation in gui
     * @param ref player reference
     * @param store player's world store
     * @param key unique key for extra value
     * @return extra value for player at this spell
     * 
     * @see Spell#setExtra(SpellContext, Ref, Store, String, BsonValue)
     */
    @NullableDecl
    public BsonValue getExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        if (!this.has(context, ref, store))
            return null;
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        return component.getExtra(context, key);
    }

    /**
     * Sets extra data for player
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, BsonValue value) {
        if (!this.has(context, ref, store))
            throw new IllegalArgumentException("Cannot set extra data for unlearned spell use spell.has() before spell.setExtra()");
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        component.setExtra(context, key, value);
    }

    /**
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    public String  getExtraString( SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asString().getValue();
    }
    /**
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    public int     getExtraInt(    SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asInt32().getValue();
    }
    /**
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    public double  getExtraDouble( SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asDouble().getValue();
    }
    /**
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    public boolean getExtraBoolean(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asBoolean().getValue();
    }

    /**
     * @see Spell#setExtra(SpellContext, Ref, Store, String, String)
     */
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, String value) {
        this.setExtra(context, ref, store, key, new BsonString(value));
    }
    /**
     * @see Spell#setExtra(SpellContext, Ref, Store, String, String)
     */
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, int value) {
        this.setExtra(context, ref, store, key, new BsonInt32(value));
    }
    /**
     * @see Spell#setExtra(SpellContext, Ref, Store, String, String)
     */
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, double value) {
        this.setExtra(context, ref, store, key, new BsonDouble(value));
    }
    /**
     * @see Spell#setExtra(SpellContext, Ref, Store, String, String)
     */
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, boolean value) {
        this.setExtra(context, ref, store, key, new BsonBoolean(value));
    }

    // TODO Predicate<String> validator
    // TODO String description
    /**
     * SpellFields are per spell data
     * use in constructor
     *
     * @param name unique key
     * @param codec Codec
     * @return spell field container
     * @param <T> data type
     */
    protected <T> SpellField<T> requireField(String name, Codec<T> codec) {
        SpellField<T> field = new SpellField<>(name, codec);
        this.fields.put(name, field);
        return field;
    }
    /**
     * @see Spell#requireField(String, Codec)
     */
    protected SpellField<String> requireFieldString(String name) {
        return this.requireField(name, Codec.STRING);
    }
    /**
     * @see Spell#requireField(String, Codec)
     */
    protected SpellField<Integer> requireFieldInt(String name) {
        return this.requireField(name, Codec.INTEGER);
    }
    /**
     * @see Spell#requireField(String, Codec)
     */
    protected SpellField<Double> requireFieldDouble(String name) {
        return this.requireField(name, Codec.DOUBLE);
    }
    /**
     * @see Spell#requireField(String, Codec)
     */
    protected SpellField<Boolean> requireFieldBoolean(String name) {
        return this.requireField(name, Codec.BOOLEAN);
    }
    /**
     * @see Spell#requireField(String, Codec)
     */
    protected <E extends Enum<E>> SpellField<E> requireFieldEnum(String name, Class<E> eClass) {
        return this.requireField(name, new EnumCodec<>(eClass));
    }

    /// @return spell Name
    public String getName() {
        return name;
    }
    /// @return Spell description
    public String getDescription() {
        return description;
    }

    /// @return Easily access to spell registry
    public static SpellRegistry getSpellRegistry() {
        return HySpellEnginePlugin.getInstance().getSpellRegistry();
    }
}
