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
import me.jomi.hyspellengine.spells.StatsSpell;
import me.jomi.hyspellengine.utils.Adapter;
import me.jomi.hyspellengine.utils.UIBuilder;
import org.bson.*;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>Spell mechanics</br>
 * note that, Spell class is Spell core, not in-gui spell</br>
 * contains data needed to make spell in admin tool</br>
 * supports learn/unlearn mechanic</p>
 * 
 * @see SpellContext
 * @see Spell#requireField(String, String, Codec, Function, Function)
 * @see Spell#getExtra(SpellContext, Ref, Store, String) 
 */
public abstract class Spell {
    protected final String name;
    protected final String description;
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
     * Optionally for Override</br>
     * called after making new spell in admin tool
     * @param context spell representation in gui
     * @return true if spell is valid overrise false
     * @throws Throwable same result as returning false
     *
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    public boolean validate(SpellContext context) throws Throwable {
        return true;
    }

    /**
     * Optionally for Override
     *
     * @param spellContext spell representation in gui. Use SpellField.getValue(spellContext) for fields values
     * @param ref          player reference
     * @param store        player's world store
     * @param ui           ui builder
     */
    public void build(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store, UIBuilder ui) {
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
        return !this.has(spellContext, ref, store);
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
        return spellContext.isLearned(ref, store);
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
    /// true if it has extra value with this key
    public boolean hasExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        BsonValue extra = this.getExtra(context, ref, store, key);
        return extra != null;
    }


    /**
     * SpellFields are per spell data</br>
     * use it in constructor
     *
     * @param name unique key
     * @param description Tooltip in admin tool
     * @param codec Codec
     * @param asString T -> String for getting in admin tool
     * @param fromString String -> T for setting in admin tool, throw any Throwable if is not valid
     * @return spell field container
     * @param <T> data type
     *
     * @see SpellField
     * @see Spell#getExtra(SpellContext, Ref, Store, String)
     */
    protected final <T> SpellField<T> requireField(String name, String description, Codec<T> codec, Function<T, String> asString, Function<String, T> fromString) {
        SpellField<T> field = new SpellField<>(name, description, codec, asString, fromString);
        if (this.fields.containsKey(name))
            throw new IllegalArgumentException("fields names must be unique: " + name);
        this.fields.put(name, field);
        return field;
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final <T> SpellField<T> requireField(String name, String description, Codec<T> codec, Function<String, T> fromString) {
        return this.requireField(name, description, codec, String::valueOf, fromString);
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<String> requireFieldString(String name, String description) {
        return this.requireField(name, description, Codec.STRING, s -> s);
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<String> requireFieldString(String name, String description, Predicate<String> validator) {
        return this.requireField(name, description, Codec.STRING, s -> {
            if (validator.test(s))
                return s;
            throw new RuntimeException();
        });
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<Integer> requireFieldInt(String name, String description) {
        return this.requireField(name, description, Codec.INTEGER, Integer::parseInt);
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<Integer> requireFieldInt(String name, String description, Predicate<Integer> validator) {
        return this.requireField(name, description, Codec.INTEGER, s -> {
            Integer n = Integer.parseInt(s);
            if (validator.test(n))
                return n;
            throw new RuntimeException();
        });
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<Double> requireFieldDouble(String name, String description) {
        return this.requireField(name, description, Codec.DOUBLE, Adapter::formatDouble, Double::parseDouble);
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<Double> requireFieldDouble(String name, String description, Predicate<Double> validator) {
        return this.requireField(name, description, Codec.DOUBLE, str -> {
            Double n = Double.parseDouble(str);
            if (validator.test(n))
                return n;
            throw new RuntimeException();
        });
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final SpellField<Boolean> requireFieldBoolean(String name) {
        return this.requireField(name, description, Codec.BOOLEAN, Boolean::parseBoolean);
    }
    /**
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    protected final <E extends Enum<E>> SpellField<E> requireFieldEnum(String name, String description, Class<E> eClass) {
        return this.requireField(name, description, new EnumCodec<>(eClass), str -> {
            try {
                return Adapter.cast(eClass.getMethod("valueOf", String.class).invoke(null, str));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Returns required fields for spell
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    public final Collection<SpellField<?>> getFields() {
        return this.fields.values();
    }
    /** Returns required fields ids
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    public final Set<String> getFieldsKeys() {
        return this.fields.keySet();
    }
    /**
     * Not recommended to use
     * @param name SpellField name
     * @return SpellField registered with requireField
     * @see Spell#requireField(String, String, Codec, Function, Function)
     */
    public final <T> SpellField<T> getField(String name) {
        return (SpellField<T>) this.fields.get(name);
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
