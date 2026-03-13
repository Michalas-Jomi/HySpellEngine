package me.jomi.hyspellengine.core;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import me.jomi.hyspellengine.utils.Adapter;

import java.util.function.Function;

/**
 * data per skill
 *
 * @param name unique name of field
 * @param description tooltip in admin tool
 * @param codec Codec for io
 * @param asString getter for admin tool
 * @param fromString setter & validator for admin tool, should throw any Throwable if value is not valid
 * @param <T> type of data
 *
 * @see me.jomi.hyspellengine.api.Spell Spell
 * @see me.jomi.hyspellengine.api.Spell#requireField(String, String, Codec, Function, Function) Spell.requireField
 * @see SpellContext
 */
public record SpellField<T> (String name, String description, Codec<T> codec, Function<T, String> asString, Function<String, T> fromString) {
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
