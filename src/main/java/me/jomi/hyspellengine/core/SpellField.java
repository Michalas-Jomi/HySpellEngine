package me.jomi.hyspellengine.core;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import me.jomi.hyspellengine.utils.Adapter;

import java.util.function.Function;

public record SpellField<T> (String name, Codec<T> codec, Function<T, String> asString, Function<String, T> fromString) {
    public T getValue(SpellContext context) {
        if (!context.getFieldsData().containsKey(this.name()))
            return this.isBoolean() ? Adapter.cast(false) : null;
        return this.codec().decode(context.getFieldsData().get(this.name()));
    }

    public String asString(SpellContext context) {
        T value = this.getValue(context);
        return value == null ? "" : this.asString.apply(value);
    }
    public T fromString(String str) throws Throwable {
        return this.fromString.apply(str);
    }

    public boolean isBoolean() {
        return this.codec == Codec.BOOLEAN;
    }
    public boolean isEnum() {
        return this.codec instanceof EnumCodec;
    }
}
