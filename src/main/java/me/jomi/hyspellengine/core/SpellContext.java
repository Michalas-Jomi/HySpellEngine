package me.jomi.hyspellengine.core;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class SpellContext {
    public static record Display(String name, String description, Path icon) {
    }
    public static class SpellComponent implements Component<EntityStore> {
        public static final BuilderCodec<SpellComponent> CODEC = EasyCodec.create(SpellComponent.class);

        public static ComponentType<EntityStore, SpellComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(SpellComponent.class);
        }

        //     spells   spellContext  extra
        // Map<String, Map<UUID, Map<String, BsonValue>>>
        @EasyCodec.ForCodec public BsonDocument spells = new BsonDocument();

        public BsonDocument getSpellMap(Spell spell) {
            return this.spells.get(spell.getName()).asDocument();
        }

        public BsonDocument getOrCreateSpellMap(Spell spell) {
            BsonDocument bson = this.getSpellMap(spell);
            if (bson == null) {
                bson = new BsonDocument();
                this.spells.put(spell.getName(), bson);
            }
            return bson;
        }

        public void addSpell(@NonNullDecl SpellContext spell, @NullableDecl BsonDocument extra) {
            if (extra == null)
                extra = new BsonDocument();

            BsonDocument spellMap = this.getOrCreateSpellMap(spell.getSpell());
            spellMap.put(spell.getUuid().toString(), extra);
        }

        public boolean removeSpell(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.getSpell());
            if (spellMap == null)
                return false;

            return spellMap.remove(spell.getUuid().toString()) != null;
        }

        public boolean removeSpell(@NonNullDecl Spell spell) {
            return this.spells.remove(spell.getName()) != null;
        }

        public boolean hasSpell(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.getSpell());
            if (spellMap == null)
                return false;
            return spellMap.containsKey(spell.getUuid().toString());
        }

        public boolean hasSpell(@NonNullDecl Spell spell) {
            return this.spells.containsKey(spell.getName());
        }

        private BsonDocument getExtra(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.getSpell());
            if (spellMap == null)
                return null;
            return spellMap.get(spell.getUuid().toString()).asDocument();
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
            copy.spells = this.spells.clone();
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
            if (child.parent != null)
                throw new IllegalArgumentException("SpellContext can't has more than 1 parent");
            child.parent = this;
        }
    }

    public SpellContext[] getChildren() {
        return children;
    }
    public BsonDocument getFields() {
        return fields;
    }
    public UUID getUuid() {
        return uuid;
    }
    public Display getDisplay() {
        return display;
    }
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

    void setCategory(Category category) {
        this.category = category;
        for (SpellContext child : this.children)
            child.setCategory(category);
    }
}
