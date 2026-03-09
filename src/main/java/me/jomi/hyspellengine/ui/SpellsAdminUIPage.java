package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;
import me.jomi.hyspellengine.utils.Adapter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SpellsAdminUIPage extends SpellsUIPage {
    public SpellsAdminUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }

    public static final String LAYOUT_SPELL = "HySpellEngine/Spells/Admin/Spell.ui";
    public static final String LAYOUT_FIELDS = "HySpellEngine/Spells/Admin/Fields.ui";
    public static final String LAYOUT_CATEGORY = "HySpellEngine/Spells/Admin/Category.ui";
    public static final String LAYOUT_FIELD_ENUM = "HySpellEngine/Spells/Admin/EnumField.ui";
    public static final String LAYOUT_FIELD_STRING = "HySpellEngine/Spells/Admin/StringField.ui";
    public static final String LAYOUT_FIELD_BOOLEAN = "HySpellEngine/Spells/Admin/BooleanField.ui";
    public static final String LAYOUT_EXPERIENCE = ""; // TODO
    public static final String LAYOUT_EXPERIENCE_VALUE = "HySpellEngine/Spells/Admin/ExperienceValue.ui";
    public static final String LAYOUT_EXPERIENCE_VALUES = "HySpellEngine/Spells/Admin/ExperienceValues.ui";
    public static final String LAYOUT_EXPERIENCE_LEVEL = "HySpellEngine/Spells/Admin/ExperienceLevel.ui";
    public static final String LAYOUT_EXPERIENCE_LEVELS = "HySpellEngine/Spells/Admin/ExperienceLevels.ui";
    public static final String LAYOUT_EXPERIENCE_EDIT_BUTTON = "HySpellEngine/Spells/Admin/ExperienceEditButton.ui";

    public static final Value<?> VALUE_FIELD_LABEL_STYLE = Value.ref(LAYOUT_FIELDS, "FieldLabelStyle");

    protected Experience experience;

    @Override
    protected void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        super.openSpells(ref, store, ui, events);

        int i = -1;
        for (Category category : Data.getCategories()) {
            String selector = "#Categories[" + ++i + "] ";
            events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + "#CategoryButton",
                    EventData.of("ACTION", "openEditCategory")
                            .put("CATEGORY", String.valueOf(i)));
        }
    }
    @Override
    protected void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Category category) {
        super.openCategory(ref, store, ui, events, category);
    }
    @Override
    protected void addSpell(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, SpellContext spell, String selector) {
        super.addSpell(ref, store, ui, events, spell, selector);

        events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + "#Parent #SpellButton", EventData
                .of("ACTION", "spell")
                .put("SPELL", spell.getUuid().toString())
                .put("SELECTOR", selector)
        );
    }

    @Override
    protected void openExperience(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Experience experience, String selector) {
        super.openExperience(ref, store, ui, events, experience, selector);

        ui.append(selector.trim(), LAYOUT_EXPERIENCE_EDIT_BUTTON);

        for (CustomUIEventBindingType type : new CustomUIEventBindingType[]{CustomUIEventBindingType.Activating, CustomUIEventBindingType.RightClicking})
            events.addEventBinding(type, selector + "#ExperienceEditButton", EventData
                    .of("ACTION", "openExperienceEdit")
                    .put("EXPERIENCE", experience.getName())
                    .put("SELECTOR", selector)
            );

    }


    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.action) {
            case "openEditCategory" -> this.handleDataEventOpenEditCategory(ref, store, data);
            case "openExperienceEdit" -> this.handleDataEventOpenEditExperiences(ref, store, data);
            default -> super.handleDataEvent(ref, store, data);
        }
    }

    private void handleDataEventOpenEditExperiences(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        Experience experience = Experience.getRegistry().getExperience(data.experience);
        this.experience = experience;

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_VALUES);
        ui.set("#ExperienceValuesRoot #Description.Text", experience.getInfo());
        AtomicInteger i = new AtomicInteger(0);
        experience.forEachValue((value, exp) -> {
            ui.append("#ExperienceValuesRoot #Values", LAYOUT_EXPERIENCE_VALUE);
            String selector = "#ExperienceValuesRoot #Values[" + i.getAndIncrement() + "] ";

            ui.set(selector + "#InputKey.Value", value);
            ui.set(selector + "#InputValue.Value", String.valueOf(exp));
        });

        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_LEVELS);
        i.set(0);
        double lastExp = 0;
        for (Experience.Level level : experience.getLevels()) {
            if (level.infinite())
                break;
            ui.append("#ExperienceLevelsRoot #Levels", LAYOUT_EXPERIENCE_LEVEL);
            String selector = "#ExperienceLevelsRoot #Levels[" + i.getAndIncrement() + "] ";

            ui.set(selector + "#FieldLabel.Text", i.get());
            ui.set(selector + "#Input.Value", String.valueOf(level.exp() - lastExp));
            lastExp -= level.exp();
        }
        if (experience.isInfinite()) {
            ui.set("#ExperienceLevelsRoot #CheckBox.Value", true);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup.Visible", true);

        }

        sendUpdate(ui, events, false);
    }

    private void handleDataEventOpenEditCategory(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        Category category = Data.getCategories()[Integer.valueOf(data.category)];
        String selector = "#Categories[" + data.category + "] ";

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        ui.clear(selector.trim());
        ui.append(selector.trim(), LAYOUT_CATEGORY);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getExperienceRegistry().forEach((id, exp) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set(selector + "#ExperienceGroup #ExperienceInput.Value", category.experience().getName());
        ui.set(selector + "#ExperienceGroup #ExperienceInput.Entries", values);

        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[0] #FieldLabel.Text", "Name");
        ui.set(selector + "#DisplayGroup[0] #Input.PlaceholderText", "Name");
        ui.set(selector + "#DisplayGroup[0] #Input.Value", category.display().name());
        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[1] #FieldLabel.Text", "Desc");
        ui.set(selector + "#DisplayGroup[1] #Input.PlaceholderText", "Description");
        ui.set(selector + "#DisplayGroup[1] #Input.Value", category.display().description());
        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[2] #FieldLabel.Text", "Icon");
        ui.set(selector + "#DisplayGroup[2] #Input.PlaceholderText", "Icon");
        ui.set(selector + "#DisplayGroup[2] #Input.Value", category.display().icon().toString().replace('\\', '/'));

        sendUpdate(ui, events, false);
    }

    @Override
    protected void handleDataEventSpell(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        SpellContext spell = super.category.getSpell(UUID.fromString(data.spell));

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        ui.clear(data.selector + "#Parent");
        ui.append(data.selector + "#Parent", LAYOUT_SPELL);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getSpellRegistry().forEach((id, s) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set(data.selector + "#Parent #SpellTypeInput.Value", spell.getSpell().getName());
        ui.set(data.selector + "#Parent #SpellTypeInput.Entries", values);

        ui.append(data.selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(data.selector + "#Parent #DisplayGroup[0] #FieldLabel.Text", "Name");
        ui.set(data.selector + "#Parent #DisplayGroup[0] #Input.PlaceholderText", "Name");
        ui.set(data.selector + "#Parent #DisplayGroup[0] #Input.Value", spell.getDisplay().name());
        ui.append(data.selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(data.selector + "#Parent #DisplayGroup[1] #FieldLabel.Text", "Desc");
        ui.set(data.selector + "#Parent #DisplayGroup[1] #Input.PlaceholderText", "Description");
        ui.set(data.selector + "#Parent #DisplayGroup[1] #Input.Value", spell.getDisplay().description());
        ui.append(data.selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(data.selector + "#Parent #DisplayGroup[2] #FieldLabel.Text", "Icon");
        ui.set(data.selector + "#Parent #DisplayGroup[2] #Input.PlaceholderText", "Icon");
        ui.set(data.selector + "#Parent #DisplayGroup[2] #Input.Value", spell.getDisplay().icon().toString().replace('\\', '/'));


        if (spell.getFieldsData() != null) {
            AtomicInteger index = new AtomicInteger(0);
            spell.getSpell().getFieldsKeys().stream().sorted().forEach(fieldName -> {
                SpellField<?> field = spell.getSpell().getField(fieldName);
                String selector = data.selector + "#Parent #FieldsGroup[" + index.getAndIncrement() + "] ";

                if (field.isBoolean()) {
                    ui.append(data.selector + "#Parent #FieldsGroup", LAYOUT_FIELD_BOOLEAN);
                    ui.set(selector + "#Input.Value", (boolean) field.getValue(spell));

                } else if (field.isEnum()) {
                    ui.append(data.selector + "#Parent #FieldsGroup", LAYOUT_FIELD_ENUM);
                    this.buildEnum(spell, field, ui, events, data, selector);
                } else {
                    ui.append(data.selector + "#Parent #FieldsGroup", LAYOUT_FIELD_STRING);
                    ui.set(selector + "#Input.PlaceholderText", fieldName);
                    ui.set(selector + "#Input.Value", field.asString(spell));
                }
                ui.set(selector + "#FieldLabel.Text", fieldName);
                ui.set(selector + "#FieldLabel.Style", VALUE_FIELD_LABEL_STYLE);
            });
        }

        sendUpdate(ui, events, false);
    }
    private void buildEnum(SpellContext spell, SpellField<?> field, UICommandBuilder ui, UIEventBuilder events, SpellsUIEventData data, String selector) {
        try {
            this.buildEnum0(spell, field, ui, events, data, selector);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    private void buildEnum0(SpellContext spell, SpellField<?> field, UICommandBuilder ui, UIEventBuilder events, SpellsUIEventData data, String selector) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> enumClass = this.getEnumClass(field);

        List<DropdownEntryInfo> entries = new ArrayList<>();
        Enum<?>[] values = Adapter.cast(enumClass.getMethod("values").invoke(null));
        for (Enum<?> en : values) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(en.toString()), en.name()));
        }

        ui.set(selector + "#Input.Entries", entries);
        ui.set(selector + "#Input.Value", ((Enum<?>) field.getValue(spell)).name());
    }
    private Class<Enum<?>> getEnumClass(SpellField<?> field) throws NoSuchFieldException, IllegalAccessException {
        Field clazz = EnumCodec.class.getDeclaredField("clazz");
        clazz.setAccessible(true);
        return Adapter.cast(clazz.get(field.codec()));
    }
}
