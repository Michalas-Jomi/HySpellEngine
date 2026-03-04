package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Spell;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.nio.file.Path;
import java.util.UUID;

public record SpellContext(Category category, @NullableDecl SpellContext parent, Spell spell, Display display, UUID uuid, BsonDocument fields, SpellContext[] children) {
    public static record Display(String name, String description, Path icon, int x, int y) {
    }
}
