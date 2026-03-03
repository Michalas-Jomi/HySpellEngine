package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

public class Editor<T> extends InteractiveCustomUIPage<Editor.EditorData> {
    public static class EditorData {
        public static final BuilderCodec<EditorData> CODEC = EasyCodec.create(EditorData.class);

        @EasyCodec.ForCodec public String action;
        @EasyCodec.ForCodec public String field;
        @EasyCodec.ForCodec(dynamic = true) public String value;
    }
    @Target(value= ElementType.FIELD)
    @Retention(value= RetentionPolicy.RUNTIME)
    public static @interface Display {
        String name() default "";
        String description() default "";
    }

    private static final String LAYOUT = "HySpellEngine/Editor/Editor.ui";
    private static final String LAYOUT_STRING_INPUT = "HySpellEngine/Editor/EditorEntryString.ui";
    private static final String LAYOUT_BOOLEAN_INPUT = "HySpellEngine/Editor/EditorEntryBoolean.ui";


    private final T value;
    private final T originValue;

    private BiConsumer<T, T> onCancel;
    private Consumer<T> onSave;
    private Predicate<T> onValidate;

    private final Set<String> failedFields = new HashSet<>();


    public Editor(PlayerRef playerRef, T originValue, T value, BiConsumer<T, T> onCancel, Consumer<T> onSave, Predicate<T> onValidate) {
        super(playerRef, CustomPageLifetime.CanDismiss, EditorData.CODEC);
        this.originValue = originValue;
        this.value = value;
        this.onCancel = onCancel;
        this.onValidate = onValidate;
        this.onSave = onSave;
    }
    public Editor(@NonNullDecl PlayerRef playerRef, T value) {
        T originValue;
        try {
            BuilderCodec<T> CODEC = (BuilderCodec<T>) value.getClass().getDeclaredField("CODEC").get(null);
            BsonValue encoded = CODEC.encode(value);
            originValue = CODEC.decode(encoded);
            value = CODEC.decode(encoded);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        this(playerRef, originValue, value, null, null, null);
    }

    public void onSave(Consumer<T> onSave) {
        this.onSave = onSave;
    }
    public void onCancel(BiConsumer<T, T> onCancel) {
        this.onCancel = onCancel;
    }
    public void onValidate(Predicate<T> onValidate) {
        this.onValidate = onValidate;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        ui.append(LAYOUT);
        ui.set("#TitleLabel.TextSpans", Message.raw("Editor " + this.value.getClass().getSimpleName()));

        int index = 0;
        for (Field field : EasyCodec.getFields(value.getClass())) {
            this.buildField(field, index++, ui, events);
        }
        // empty space at bottom of scrollbar
        ui.appendInline("#Fields", """
                Group {
                  Anchor: (Width: 1000, Height: 200);
                  Background: #ff00ff(0.0);
                }""");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ButtonSave",
                EventData.of("ACTION", "save")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ButtonValidate",
                EventData.of("ACTION", "validate")
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ButtonCancel",
                EventData.of("ACTION", "cancel")
        );
    }
    private void buildField(Field field, int index, UICommandBuilder ui, UIEventBuilder events) {
        String selector = "#Fields[" + index + "]";

        Class<?> clazz = field.getType();
        if (clazz == boolean.class || clazz == Boolean.class)
            this.buildBooleanField(selector + " ", field, ui, events);
        else
            this.buildStringField(selector + " ", field , ui, events);

        ui.set(selector + " #FieldLabel.Text", Editor.title(field));
        if (field.isAnnotationPresent(Display.class) && !field.getAnnotation(Display.class).description().isBlank())
            ui.set(selector + ".TooltipText", field.getAnnotation(Display.class).description());
    }
    private void buildStringField(String selector, Field field, UICommandBuilder ui, UIEventBuilder events) {
        ui.append("#Fields", LAYOUT_STRING_INPUT);
        ui.set(selector + "#FieldInput.Value", Editor.getAsString(this.getValue(field)));
        ui.set(selector + "#FieldInput.PlaceholderText", Editor.title(field));

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + "#FieldInput",
                EventData.of(
                        "ACTION", "set").put(
                        "@VALUE", selector + "#FieldInput.Value").put(
                        "FIELD", field.getName())
        );
    }
    private void buildBooleanField(String selector, Field field, UICommandBuilder ui, UIEventBuilder events) {
        ui.append("#Fields", LAYOUT_BOOLEAN_INPUT);
        ui.set(selector + "#FieldInput.Value", (boolean) this.getValue(field));

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                selector + "#FieldInput",
                EventData.of(
                        "ACTION", "negate").put(
                        "FIELD", field.getName())
        );
    }

    public static String title(Field field) {
        if (field.isAnnotationPresent(Display.class)) {
            String display = field.getAnnotation(Display.class).name();
            if (!display.isBlank())
                return display;
        }

        String result =  field.getName().replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private static String getAsString(Object obj) {
        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();

            int le = Array.getLength(obj);
            for (int i=0; i < le; i++) {
                sb.append(getAsString(Array.get(obj, i)));
                if (i != le-1)
                    sb.append(", ");
            }

            return sb.toString();
        }
        return String.valueOf(obj);
    }

    private Object getValue(Field field) {
        try {
            return field.get(this.value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EditorData data) {
        if ("set".equals(data.action)) {
            handleDataEventSet(data);
        } else if ("negate".equals(data.action)) {
            handleDataEventNegate(data);
        } else if ("save".equals(data.action)) {
            if (this.onSave != null) {
                if (!this.failedFields.isEmpty() || (this.onValidate != null && !this.onValidate.test(this.value))) {
                    playerRef.sendMessage(Message.raw("Validate failed"));
                    this.sendUpdate();
                    return;
                } else {
                    this.onSave.accept(this.value);
                }
            }
            close();
        } else if ("cancel".equals(data.action)) {
            if (this.onCancel != null)
                this.onCancel.accept(this.originValue, this.value);
            close();
        } else if ("validate".equals(data.action)) {
            if (!this.failedFields.isEmpty() || (this.onValidate != null && !this.onValidate.test(this.value))) {
                playerRef.sendMessage(Message.raw("Validate failed"));
            } else {
                playerRef.sendMessage(Message.raw("Validate successful"));
            }
            this.sendUpdate();
        }
    }
    private void handleDataEventNegate(EditorData data) {
        try {
            Field field = this.value.getClass().getDeclaredField(data.field);
            field.set(this.value, !((boolean) field.get(this.value)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.sendUpdate();
    }
    private void handleDataEventSet(EditorData data) {
        UICommandBuilder ui = new UICommandBuilder();

        int index = 0;
        for (Field field : EasyCodec.getFields(this.value.getClass())) {
            if (field.getName().equals(data.field))
                break;
            index++;
        }

        String selector = "#Fields[" + index + "] ";

        try {
            Field field = this.value.getClass().getDeclaredField(data.field);
            Object value = this.parseValue(field.getType(), data.value);
            field.set(this.value, value);
            ui.set(selector + "#FieldLabel.Style.TextColor", "#009900");
            ui.set(selector + "#FieldInput.Style.TextColor", "#ffffff");
            this.failedFields.remove(data.field);
        } catch (Throwable e) {
            ui.set(selector + "#FieldLabel.Style.TextColor", "#990000");
            ui.set(selector + "#FieldInput.Style.TextColor", "#bb0000");
            this.failedFields.add(data.field);
        }

        this.sendUpdate(ui);
    }
    private Object parseValue(Class<?> clazz, String value) {
        if (clazz == int.class || clazz == Integer.class) return Integer.valueOf(value);
        if (clazz == float.class || clazz == Float.class) return Float.valueOf(value);
        if (clazz == double.class || clazz == Double.class) return Double.valueOf(value);
        if (clazz == short.class || clazz == Short.class) return Short.valueOf(value);
        if (clazz == long.class || clazz == Long.class) return Long.valueOf(value);
        if (clazz == byte.class || clazz == Byte.class) return Byte.valueOf(value);
        if (clazz == boolean.class || clazz == Boolean.class) return Boolean.valueOf(value);
        if (clazz == String.class) return value;
        if (clazz == BsonDocument.class) return BsonDocument.parse(value);
        if (clazz == Duration.class) return Duration.parse(value); //  Codec.DURATION_SECONDS unused
        if (clazz == Path.class) return Path.of(value);
        if (clazz == Instant.class) return Instant.parse(value);
        if (clazz == Level.class) return Level.parse(value);
        if (clazz == UUID.class) return UUID.fromString(value); // Codec.UUID_BINARY unused

        if (clazz.isArray()) {
            clazz = clazz.getComponentType();

            if (value.isBlank())
                return Array.newInstance(clazz, 0);

            String[] elements = value.split(", ");
            Object array = Array.newInstance(clazz, elements.length);
            int i = 0;
            for (String element : elements) {
                Array.set(array, i++, parseValue(clazz, element));
            }

            return array;
        }

        throw new RuntimeException();
    }
}
