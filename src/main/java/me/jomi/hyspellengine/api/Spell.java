package me.jomi.hyspellengine.api;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;
import me.jomi.hyspellengine.core.SpellRegistry;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.bson.*;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Spell {
    private final String name;
    private final String description;
    private final Map<String, SpellField<?>> fields = new ConcurrentHashMap<>();

    public Spell(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store);
    public abstract void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store);

    public boolean has(Ref<EntityStore> ref, Store<EntityStore> store) {
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        return component != null && component.hasSpell(this);
    }


    @NullableDecl
    public BsonValue getExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        if (!this.has(ref, store))
            return null;
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        return component.getExtra(context, key);
    }
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, BsonValue value) {
        if (!this.has(ref, store))
            throw new IllegalArgumentException("Cannot set extra data for unlearned spell use spell.has() before spell.setExtra()");
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        component.setExtra(context, key, value);
    }

    public String  getExtraString( SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asString().getValue();
    }
    public int     getExtraInt(    SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asInt32().getValue();
    }
    public double  getExtraDouble( SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asDouble().getValue();
    }
    public boolean getExtraBoolean(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key) {
        return this.getExtra(context, ref, store, key).asBoolean().getValue();
    }

    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, String value) {
        this.setExtra(context, ref, store, key, new BsonString(value));
    }
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, int value) {
        this.setExtra(context, ref, store, key, new BsonInt32(value));
    }
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, double value) {
        this.setExtra(context, ref, store, key, new BsonDouble(value));
    }
    public void setExtra(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, String key, boolean value) {
        this.setExtra(context, ref, store, key, new BsonBoolean(value));
    }

    protected <T> SpellField<T> requireField(String name, Codec<T> codec) {
        SpellField<T> field = new SpellField<>(name, codec);
        this.fields.put(name, field);
        return field;
    }
    protected SpellField<String> requireFieldString(String name) {
        return this.requireField(name, Codec.STRING);
    }
    protected SpellField<Integer> requireFieldInt(String name) {
        return this.requireField(name, Codec.INTEGER);
    }
    protected SpellField<Double> requireFieldDouble(String name) {
        return this.requireField(name, Codec.DOUBLE);
    }
    protected SpellField<Boolean> requireFieldBoolean(String name) {
        return this.requireField(name, Codec.BOOLEAN);
    }
    protected <E extends Enum<E>> SpellField<E> requireFieldEnum(String name, Class<E> eClass) {
        return this.requireField(name, new EnumCodec<>(eClass));
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }

    public static SpellRegistry getSpellRegistry() {
        return HySpellEnginePlugin.getInstance().getSpellRegistry();
    }
}
