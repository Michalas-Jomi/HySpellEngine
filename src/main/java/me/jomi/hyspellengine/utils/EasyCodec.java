package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.NumberSchema;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.logger.HytaleLogger;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class EasyCodec<T> {
    @Target(value= ElementType.FIELD)
    @Retention(value= RetentionPolicy.RUNTIME)
    public static @interface ForCodec {
        boolean dynamic() default false;
    }
    protected static record ObjectMapCodec<T> (Codec<T> valueCodec) implements Codec<Map<String, T>> {
        @NullableDecl
        @Override
        public Map<String, T> decode(BsonValue bsonValue, ExtraInfo extraInfo) {
            Map<String, T> map = new HashMap<>();
            bsonValue.asDocument().forEach((key, value) -> map.put(key, this.valueCodec.decode(value, extraInfo)));
            return map;
        }

        @Override
        public BsonValue encode(Map<String, T> map, ExtraInfo extraInfo) {
            BsonDocument bson = new BsonDocument();
            map.forEach((key, value) -> bson.put(key, this.valueCodec.encode(value, extraInfo)));
            return bson;
        }

        @NonNullDecl
        @Override
        public Schema toSchema(@NonNullDecl SchemaContext schemaContext) {
            // similar to bson Codec
            return new ObjectSchema();
        }
    }
    protected static record ListCodec<T> (Codec<T> valueCodec) implements Codec<List<T>> {
        @NullableDecl
        @Override
        public List<T> decode(BsonValue bsonValue, ExtraInfo extraInfo) {
            List<T> list = new ArrayList<>();
            bsonValue.asArray().forEach(value -> list.add(this.valueCodec.decode(value, extraInfo)));
            return list;
        }

        @Override
        public BsonValue encode(List<T> list, ExtraInfo extraInfo) {
            BsonArray array = new BsonArray();
            list.forEach(value -> array.add(this.valueCodec.encode(value, extraInfo)));
            return array;
        }

        @NonNullDecl
        @Override
        public Schema toSchema(@NonNullDecl SchemaContext schemaContext) {
            ArraySchema s = new ArraySchema();
            s.setItem(this.valueCodec.toSchema(schemaContext));
            return s;
        }
    }

    public static <T> EasyCodec<T> builder(Class<T> clazz) {
        return new EasyCodec<>(clazz);
    }
    public static <T> BuilderCodec<T> create(Class<T> clazz) {
        return EasyCodec.builder(clazz).build();
    }

    private final Class<T> clazz;
    private final BuilderCodec.Builder<T> builder;
    private EasyCodec(Class<T> clazz) {
        this.clazz = clazz;
        this.builder = this.init();

    }
    private BuilderCodec.Builder<T> init() {
        BuilderCodec.Builder<T> builder = BuilderCodec.builder(clazz, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        for (Field field : EasyCodec.getFields(clazz)) {
            String id = field.getName().toUpperCase();
            if (field.getDeclaredAnnotation(ForCodec.class).dynamic())
                id = "@" + id;

            builder.append(
                    new KeyedCodec<>(id, getCodec(field.getGenericType())),
                    (d, v) -> {
                        try {
                            field.set(d, v);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    d -> {
                        try {
                            return field.get(d);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ).add();
        }

        return builder;
    }
    private Codec getCodec(Type type) {
        if (type instanceof Class clazz) {
            if (clazz == int.class || clazz == Integer.class) return Codec.INTEGER;
            if (clazz == float.class || clazz == Float.class) return Codec.FLOAT;
            if (clazz == double.class || clazz == Double.class) return Codec.DOUBLE;
            if (clazz == short.class || clazz == Short.class) return Codec.SHORT;
            if (clazz == long.class || clazz == Long.class) return Codec.LONG;
            if (clazz == byte.class || clazz == Byte.class) return Codec.BYTE;
            if (clazz == boolean.class || clazz == Boolean.class) return Codec.BOOLEAN;
            if (clazz == String.class) return Codec.STRING;
            if (clazz == BsonDocument.class) return Codec.BSON_DOCUMENT;
            if (clazz == Duration.class) return Codec.DURATION; //  Codec.DURATION_SECONDS unused
            if (clazz == Path.class) return Codec.PATH;
            if (clazz == Instant.class) return Codec.INSTANT;
            if (clazz == Level.class) return Codec.LOG_LEVEL;
            if (clazz == UUID.class) return Codec.UUID_STRING; // Codec.UUID_BINARY unused

            if (clazz.isArray()) {
                clazz = clazz.getComponentType();
                if (clazz == int.class || clazz == Integer.class) return Codec.INT_ARRAY;
                if (clazz == float.class || clazz == Float.class) return Codec.FLOAT_ARRAY;
                if (clazz == double.class || clazz == Double.class) return Codec.DOUBLE_ARRAY;
                if (clazz == long.class || clazz == Long.class) return Codec.LONG_ARRAY;
                if (clazz == byte.class || clazz == Byte.class) return Codec.BYTE_ARRAY;
                if (clazz == String.class) return Codec.STRING_ARRAY;
            }

            try {
                return (Codec) clazz.getDeclaredField("CODEC").get(null);
            } catch (Throwable e) {
            }
        }
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> clazz) {
            if (Map.class.isAssignableFrom(clazz) && parameterizedType.getActualTypeArguments()[0] == String.class)
                return EasyCodec.mapCodec(this.getCodec(parameterizedType.getActualTypeArguments()[1]));
            if (List.class.isAssignableFrom(clazz)) {
                return EasyCodec.listCodec(this.getCodec(parameterizedType.getActualTypeArguments()[0]));
            }
        }

        HytaleLogger.getLogger().at(Level.WARNING).log("unacceptable field type for @ForCodec " + this.clazz.getName() + " " + type.getTypeName());

        return null;
    }

    public BuilderCodec.Builder<T> raw() {
        return this.builder;
    }
    public BuilderCodec<T> build() {
        return this.builder.build();
    }

    private static final Map<Codec<?>, ObjectMapCodec<?>> mapCodecMap = new IdentityHashMap<>();
    public static <V> Codec<Map<String, V>> mapCodec(Codec<V> valueCodec) {
        return Adapter.cast(mapCodecMap.computeIfAbsent(valueCodec, ObjectMapCodec::new));
    }
    private static final Map<Codec<?>, ListCodec<?>> listCodecMap = new IdentityHashMap<>();
    public static <V> Codec<List<V>> listCodec(Codec<V> valueCodec) {
        return Adapter.cast(listCodecMap.computeIfAbsent(valueCodec, ListCodec::new));
    }

    public static List<Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields())
            if (field.isAnnotationPresent(ForCodec.class))
                fields.add(field);

        return fields;
    }
    public static String asString(Object obj) {
        if (obj == null)
            return "null";

        StringBuilder sb = new StringBuilder(obj.getClass().getSimpleName());
        sb.append("(");
        boolean first = true;
        for (Field field : EasyCodec.getFields(obj.getClass())) {
            if (!first)
                sb.append(", ");
            first = false;

            sb.append(field.getName()).append("=");
            try {
                if (field.getType().isArray()) {
                    sb.append("[");
                    asStringArray(sb, field.get(obj));
                    sb.append("]");
                } else
                    sb.append(field.get(obj));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return sb.toString();
    }
    private static void asStringArray(StringBuilder sb, Object array) {
        int length = Array.getLength(array);

        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(", ");

            Object value = Array.get(array, i);
            sb.append(value);
        }
    }
}
