package me.jomi.hyspellengine.core;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spell representation in gui
 * contains data about learnable spell made in admin tool and has access to player metadata per skill
 *
 * @see Spell
 * @see Spell#getExtra(SpellContext, Ref, Store, String)
 * @see Spell#requireField(String, String, Codec, Function, Function) 
 */
public final class SpellContext implements Cloneable {
    public static record Display(String name, String description, Path icon) {
    }
    public static class SpellComponent implements Component<EntityStore> {
        public static final BuilderCodec<SpellComponent> CODEC = EasyCodec.create(SpellComponent.class);

        public static ComponentType<EntityStore, SpellComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(SpellComponent.class);
        }

        //   spellContext  extra
        // Map<UUID, Map<String, BsonValue>>
        @EasyCodec.ForCodec public Map<String, BsonDocument> spells = new HashMap<>();

        public void addSpell(@NonNullDecl SpellContext spell, @NullableDecl BsonDocument extra) {
            if (extra == null)
                extra = new BsonDocument();

            this.spells.put(spell.getUuid().toString(), extra);
        }

        public boolean removeSpell(@NonNullDecl SpellContext spell) {
            return this.spells.remove(spell.getUuid().toString()) != null;
        }

        public boolean hasSpell(@NonNullDecl SpellContext spell) {
            return this.spells.containsKey(spell.getUuid().toString());
        }

        @NullableDecl
        private BsonDocument getExtra(@NonNullDecl SpellContext spell) {
            return this.spells.get(spell.getUuid().toString());
        }

        public BsonValue getExtra(@NonNullDecl SpellContext spell, String key) {
            BsonDocument extra = this.getExtra(spell);
            return extra == null ? null : extra.get(key);
        }

        public void setExtra(SpellContext spell, String key, BsonValue value) {
            this.getExtra(spell).put(key, value);
        }

        @Override
        public Component<EntityStore> clone() {
            SpellComponent copy = new SpellComponent();
            this.spells.forEach((key, extra) -> copy.spells.put(key, extra.clone()));
            return copy;
        }
    }

    Category category;
    @NullableDecl private SpellContext parent;
    private final Spell spell;
    private final Display display;
    private final UUID uuid;
    private final BsonDocument fields;
    private final SpellContext[] children;

    public SpellContext(Spell spell, Display display, UUID uuid, BsonDocument fields, SpellContext[] children) {
        this.spell = spell;
        this.display = display;
        this.uuid = uuid;
        this.fields = fields;
        this.children = children;

        for (SpellContext child : this.children) {
            child.parent = this;
        }
    }

    public SpellContext[] getChildren() {
        return children;
    }
    // fieldName : fieldData
    public BsonDocument getFieldsData() {
        return fields;
    }
    public UUID getUuid() {
        return uuid;
    }
    public Display getDisplay() {
        return display;
    }
    @NonNullDecl
    public Spell getSpell() {
        return spell;
    }
    @NullableDecl
    public SpellContext getParent() {
        return parent;
    }
    public Category getCategory() {
        return category;
    }

    public boolean isLearned(Ref<EntityStore> ref, Store<EntityStore> store) {
        SpellContext.SpellComponent component = store.getComponent(ref, SpellContext.SpellComponent.getComponentType());
        return component != null && component.hasSpell(this);
    }

    public boolean learn(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (getSpell().has(this, ref, store))
            return false;

        SpellComponent component = store.ensureAndGetComponent(ref, SpellComponent.getComponentType());
        component.addSpell(this, null);
        return true;
    }

    public boolean isParentLearned(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (this.parent == null)
            return true;
        return this.parent.isLearned(ref, store);
    }

    void setCategory(Category category) {
        this.category = category;
        for (SpellContext child : this.children)
            child.setCategory(category);
    }

    public boolean validate() {
        try {
            if (this.getDisplay().name().isBlank())
                return false;
            if (this.getDisplay().description().isBlank())
                return false;
            if (this.getDisplay().icon().toString().trim().length() < 4)
                return false;
            for (SpellField<?> field : this.getSpell().getFields()) {
                field.fromString(field.asString(this));
            }
            return this.spell.validate(this);
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public SpellContext clone() {
        SpellContext copy = new SpellContext(this.getSpell(), this.getDisplay(), this.getUuid(), this.fields.clone(), this.getChildren());
        copy.parent = this.parent;
        copy.category = this.category;
        return copy;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof SpellContext spell) {
            if (!this.getSpell().equals(spell.getSpell()))
                return false;
            if (!this.getDisplay().equals(spell.getDisplay()))
                return false;
            if (!this.getUuid().equals(spell.getUuid()))
                return false;
            if (!this.getFieldsData().equals(spell.getFieldsData()))
                return false;
            if (!Arrays.equals(this.getChildren(), spell.getChildren()))
                return false;
            return spell.parent == this.parent && spell.category == this.category;
        }
        return false;
    }

    public String toString() {
        return "SpellContext{" +
                "spell=" +  this.spell.getName() + ", " +
                "display=" + this.display + ", " +
                "uuid=" + this.uuid + ", " +
                "fields=" + this.fields.toJson() + ", " +
                "children=[" + String.join(", ", Arrays.stream(this.children).map(SpellContext::toString).toList()) + "]" +
                "}";
    }
}
