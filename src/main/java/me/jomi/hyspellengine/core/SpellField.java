package me.jomi.hyspellengine.core;

import com.hypixel.hytale.codec.Codec;

public record SpellField<T>(String name, Codec<T> codec) {
    public T getValue(SpellContext context) {
        return this.codec().decode(context.getFields().get(this.name()));
    }
}
