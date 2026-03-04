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
import java.util.UUID;

public record SpellContext(Category category, @NullableDecl SpellContext parent, Spell spell, Display display, UUID uuid, BsonDocument fields, SpellContext[] children) {
    public static record Display(String name, String description, Path icon, int x, int y) {
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

            BsonDocument spellMap = this.getOrCreateSpellMap(spell.spell());
            spellMap.put(spell.uuid().toString(), extra);
        }
        public boolean removeSpell(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.spell());
            if (spellMap == null)
                return false;

            return spellMap.remove(spell.uuid().toString()) != null;
        }
        public boolean removeSpell(@NonNullDecl Spell spell) {
            return this.spells.remove(spell.getName()) != null;
        }
        public boolean hasSpell(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.spell());
            if (spellMap == null)
                return false;
            return spellMap.containsKey(spell.uuid().toString());
        }
        public boolean hasSpell(@NonNullDecl Spell spell) {
            return this.spells.containsKey(spell.getName());
        }

        private BsonDocument getExtra(@NonNullDecl SpellContext spell) {
            BsonDocument spellMap = this.getSpellMap(spell.spell());
            if (spellMap == null)
                return null;
            return spellMap.get(spell.uuid().toString()).asDocument();
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
}
