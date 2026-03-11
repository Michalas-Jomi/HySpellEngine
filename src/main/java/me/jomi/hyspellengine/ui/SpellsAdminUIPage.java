package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;
import me.jomi.hyspellengine.utils.Adapter;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// TODO placeholderTexts
// Spell.description

// TODO bugfix - after add category -> change any category value -> cancel = opened nonexisting category
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
    public static final String LAYOUT_ADD_CATEGORY = "HySpellEngine/Spells/Admin/AddCategoryButton.ui";

    public static final Value<?> VALUE_FIELD_LABEL_STYLE = Value.ref(LAYOUT_FIELDS, "FieldLabelStyle");


    private int editedCategoryIndex = -1;
    private Category editedCategory;
    private Category newCategory = null;
    private Category oldSpellCategory;
    private SpellContext spell;


    @Override
    protected void openExperiences(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new ExperienceAdminUIPage(this.playerRef));
    }
    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        HySpellEnginePlugin.debugLog("building spells admin");
        super.build(ref, ui, events, store);
    }


    @Override
    protected void addCategory(UICommandBuilder ui, UIEventBuilder events, Category category, int index) {
        super.addCategory(ui, events, category, index);
        String selector = "#Categories[" + index + "] ";
        events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + "#CategoryButton",
                EventData.of("ACTION", "openEditCategory")
                        .put("CATEGORY", String.valueOf(index)));
    }

    @Override
    protected void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        super.openSpells(ref, store, ui, events);

        if (this.newCategory == null) {
            ui.append("#Categories", LAYOUT_ADD_CATEGORY);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#Categories #AddCategoryButtonRoot #Button", EventData
                    .of("ACTION", "addCategory")
            );
        } else {
            ui.append("#Categories", SpellsUIPage.LAYOUT_CATEGORY); // closeEditedCategory other usage
            this.addCategory(ui, events, this.newCategory, Data.getCategories().length);
        }
    }
    @Override
    protected void addSpell(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, SpellContext spell, String selector) {
        super.addSpell(ref, store, ui, events, spell, selector);

        ui.set(selector + "#Parent #SpellButton.Disabled", false);

        events.addEventBinding(CustomUIEventBindingType.RightClicking, selector + "#Parent #SpellButton", EventData
                .of("ACTION", "spell")
                .put("SPELL", spell.getUuid().toString())
                .put("SELECTOR", selector)
        );
    }

    @Override
    protected void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Category category) {
        if (this.newCategory == null || category == this.newCategory) {
            super.openCategory(ref, store, ui, events, category);
            this.spell = null;
        }
    }
    protected void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, Category category) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        this.openCategory(ref, store, ui, events, category);

        sendUpdate(ui, events, false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.action) {
            case "openEditCategory" -> this.handleDataEventOpenEditCategory(ref, store, data);
            case "categoryChange" -> this.handleDataEventCategoryChangeField(ref, store, data);
            case "categoryEdit" -> this.handleDataEventCategoryEdit(ref, store, data);
            case "addCategory" -> this.handleDataEventAddCategory(ref, store, data);

            case "spellEdit" -> this.handleDataEventSpellEdit(ref, store, data);
            case "spellTypeChange" -> this.handleDataEventSpellTypeChange(ref, store, data);
            case "spellDisplayChange" -> this.handleDataEventSpellDisplayChange(data);
            case "spellFieldChange" -> this.handleDataEventSpellFieldChange(data);

            default -> super.handleDataEvent(ref, store, data);
        }
    }

    private void closeEditedCategory() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        if (this.editedCategoryIndex == -1 || this.editedCategoryIndex >= Data.getCategories().length || this.newCategory != null) {
            if (super.category == this.newCategory)
                super.category = null;
            this.newCategory = null;
            this.openSpells(playerRef.getReference(), playerRef.getReference().getStore(), ui, events);
        } else {
            String selector = "#Categories[" + this.editedCategoryIndex + "] ";

            ui.clear(selector.trim());
            ui.append(selector.trim(), SpellsUIPage.LAYOUT_CATEGORY);
            this.addCategory(ui, events, Data.getCategories()[this.editedCategoryIndex], this.editedCategoryIndex);

            this.editedCategory = null;
            this.editedCategoryIndex = -1;
        }
        sendUpdate(ui, events, false);
    }
    private void handleDataEventOpenEditCategory(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        if (this.editedCategory != null) {
            this.closeEditedCategory();
        }

        this.editedCategoryIndex = Integer.parseInt(data.category);
        Category category = Data.getCategories()[this.editedCategoryIndex];
        String selector = "#Categories[" + data.category + "] ";
        this.editedCategory = category;

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        this.openEditCategory(category, selector, ui, events);

        sendUpdate(ui, events, false);
    }
    private void openEditCategory(Category category, String selector, UICommandBuilder ui, UIEventBuilder events) {
        if (this.newCategory != null && category != this.newCategory)
            closeEditedCategory();

        ui.clear(selector.trim());
        ui.append(selector.trim(), LAYOUT_CATEGORY);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getExperienceRegistry().forEach((id, exp) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set(selector + "#ExperienceGroup #ExperienceInput.Value", category.experience().getName());
        ui.set(selector + "#ExperienceGroup #ExperienceInput.Entries", values);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#ExperienceGroup #ExperienceInput", EventData
                .of("ACTION", "categoryChange")
                .put("META", "exp")
                .put("@VALUE", selector + "#ExperienceGroup #ExperienceInput.Value")
        );

        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[0] #FieldLabel.Text", "Name");
        ui.set(selector + "#DisplayGroup[0] #Input.PlaceholderText", "Name");
        ui.set(selector + "#DisplayGroup[0] #Input.Value", category.display().name());
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#DisplayGroup[0] #Input", EventData
                .of("ACTION", "categoryChange")
                .put("META", "Name")
                .put("@VALUE", "#DisplayGroup[0] #Input.Value")
        );

        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[1] #FieldLabel.Text", "Desc");
        ui.set(selector + "#DisplayGroup[1] #Input.PlaceholderText", "Description");
        ui.set(selector + "#DisplayGroup[1] #Input.Value", category.display().description());
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#DisplayGroup[1] #Input", EventData
                .of("ACTION", "categoryChange")
                .put("META", "Desc")
                .put("@VALUE", "#DisplayGroup[1] #Input.Value")
        );

        ui.append(selector + "#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#DisplayGroup[2] #FieldLabel.Text", "Icon");
        ui.set(selector + "#DisplayGroup[2] #Input.PlaceholderText", "Icon");
        String path = category.display().icon().toString().replace('\\', '/');
        ui.set(selector + "#DisplayGroup[2] #Input.Value", path.length() > 3 ? path : ""); // Assets path <= 3 -> client crash
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#DisplayGroup[2] #Input", EventData
                .of("ACTION", "categoryChange")
                .put("META", "Icon")
                .put("@VALUE", "#DisplayGroup[2] #Input.Value")
        );

        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#NavGroup #CancelButton", EventData
                .of("ACTION", "categoryEdit")
                .put("META", "Cancel")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#NavGroup #RemoveButton", EventData
                .of("ACTION", "categoryEdit")
                .put("META", "Remove")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#NavGroup #SaveButton", EventData
                .of("ACTION", "categoryEdit")
                .put("META", "Save")
        );
    }
    private void handleDataEventCategoryEdit(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.meta) {
            case "Cancel":
                break;
            case "Save":
                if (this.editedCategoryIndex != -1 && this.newCategory == null) {
                    Data.removeCategory(Data.getCategories()[this.editedCategoryIndex]);
                    Data.addCategory(editedCategory, this.editedCategoryIndex);
                } else if (this.newCategory != null) {
                    this.newCategory = new Category(
                            this.newCategory.display(),
                            this.newCategory.experience(),
                            this.spell
                    );
                    HySpellEnginePlugin.debugLog(this.newCategory);
                    if (this.validateNewCategory())
                        Data.addCategory(this.newCategory);
                    else {
                        playerRef.sendMessage(Message.raw("Invalid data in category or root spell"));
                        sendUpdate();
                        return;
                    }
                    super.category = this.newCategory;
                    this.newCategory = null;
                } else
                    throw new RuntimeException("Unknown category to save");
                break;
            case "Remove":
                Data.removeCategory(Data.getCategories()[this.editedCategoryIndex]);
                UICommandBuilder ui = new UICommandBuilder();
                UIEventBuilder events = new UIEventBuilder();

                if (super.category != null && super.category.root().equals(this.editedCategory.root()))
                    super.category = null;

                this.openSpells(ref, store, ui, events);

                sendUpdate(ui, events, false);
                return;
        }

        this.closeEditedCategory();
    }
    private boolean validateNewCategory() {
        if (this.newCategory == null)
            return false;
        if (this.newCategory.display().name().isBlank())
            return false;
        if (this.newCategory.display().description().isBlank())
            return false;
        if (this.newCategory.display().icon().toString().trim().length() < 4)
            return false;
        return this.newCategory.root().validate();
    }
    private void handleDataEventCategoryChangeField(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.meta) {
            case "Name" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                data.value,
                                this.editedCategory.display().description(),
                                this.editedCategory.display().icon()
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root()
                    );
            case "Desc" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                this.editedCategory.display().name(),
                                data.value,
                                this.editedCategory.display().icon()
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root()
                    );
            case "Icon" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                this.editedCategory.display().name(),
                                this.editedCategory.display().description(),
                                Path.of(data.value.length() > 3 ? data.value : "")
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root()
                    );
            case "exp" ->
                    this.editedCategory = new Category(
                            this.editedCategory.display(),
                            Experience.getRegistry().getExperience(data.value),
                            this.editedCategory.root()
                    );

        }

        if (this.newCategory != null)
            this.newCategory = this.editedCategory;

        sendUpdate();
    }
    private void handleDataEventAddCategory(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        this.newCategory = new Category(
                new Category.Display("", "", Path.of("")),
                Experience.getRegistry().getExperience(Experience.getRegistry().getKeys().stream().findAny().get()),
                new SpellContext(
                        Spell.getSpellRegistry().getSpell(Spell.getSpellRegistry().getKeys().stream().findAny().get()),
                        new SpellContext.Display("", "", Path.of("")),
                        UUID.randomUUID(),
                        new BsonDocument(),
                        new SpellContext[0]
                )
        );

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        //this.addCategory(ui, events, this.newCategory, Data.getCategories().length);
        //this.openCategory(ref, store, ui, events, this.newCategory);
        super.category = this.newCategory;
        this.editedCategory = this.newCategory;
        this.openSpells(ref, store, ui, events);

        sendUpdate(ui, events, false);

        ui = new UICommandBuilder();
        events = new UIEventBuilder();

        String selector = "#Categories[" + Data.getCategories().length + "] ";
        this.openEditCategory(category, selector, ui, events);
        this.openSpellEdit(ref, store, this.newCategory.root(), "#Spells ", ui, events);

        ui.set(selector + "#RemoveButton.Disabled", true);

        sendUpdate(ui, events, false);
    }

    @Override
    protected void handleDataEventSpell(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        SpellContext spell = super.category.getSpell(UUID.fromString(data.spell));
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        this.openSpellEdit(ref, store, spell, data.selector, ui, events);

        sendUpdate(ui, events, false);
    }
    protected void openSpellEdit(Ref<EntityStore> ref, Store<EntityStore> store, SpellContext spell, String selector, UICommandBuilder ui, UIEventBuilder events) {
        if (this.spell != null) {
            this.openCategory(ref, store, super.category);
        }

        this.spell = spell.clone();
        String uuid = spell.getUuid().toString();

        ui.clear(selector + "#Parent");
        ui.append(selector + "#Parent", LAYOUT_SPELL);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getSpellRegistry().forEach((id, s) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set(selector + "#Parent #SpellTypeInput.Value", spell.getSpell().getName());
        ui.set(selector + "#Parent #SpellTypeInput.Entries", values);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#Parent #SpellTypeInput", EventData
                .of("ACTION", "spellTypeChange")
                .put("SPELL", uuid)
                .put("SELECTOR", selector)
                .put("@VALUE", selector + "#Parent #SpellTypeInput.Value")
        );

        ui.append(selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#Parent #DisplayGroup[0] #FieldLabel.Text", "Name");
        ui.set(selector + "#Parent #DisplayGroup[0] #Input.PlaceholderText", "Name");
        ui.set(selector + "#Parent #DisplayGroup[0] #Input.Value", spell.getDisplay().name());
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#Parent #DisplayGroup[0] #Input", EventData
                .of("ACTION", "spellDisplayChange")
                .put("SPELL", spell.getUuid().toString())
                .put("META", "Name")
                .put("@VALUE", selector + "#Parent #DisplayGroup[0] #Input.Value")
        );

        ui.append(selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#Parent #DisplayGroup[1] #FieldLabel.Text", "Desc");
        ui.set(selector + "#Parent #DisplayGroup[1] #Input.PlaceholderText", "Description");
        ui.set(selector + "#Parent #DisplayGroup[1] #Input.Value", spell.getDisplay().description());
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#Parent #DisplayGroup[1] #Input", EventData
                .of("ACTION", "spellDisplayChange")
                .put("SPELL", uuid)
                .put("META", "Desc")
                .put("@VALUE", selector + "#Parent #DisplayGroup[1] #Input.Value")
        );

        ui.append(selector + "#Parent #DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set(selector + "#Parent #DisplayGroup[2] #FieldLabel.Text", "Icon");
        ui.set(selector + "#Parent #DisplayGroup[2] #Input.PlaceholderText", "Icon");
        ui.set(selector + "#Parent #DisplayGroup[2] #Input.Value", spell.getDisplay().icon().toString().replace('\\', '/'));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#Parent #DisplayGroup[2] #Input", EventData
                .of("ACTION", "spellDisplayChange")
                .put("SPELL", uuid)
                .put("META", "Icon")
                .put("@VALUE", selector + "#Parent #DisplayGroup[2] #Input.Value")
        );


        if (spell.getFieldsData() != null) {
            AtomicInteger index = new AtomicInteger(0);
            spell.getSpell().getFieldsKeys().stream().sorted().forEach(fieldName -> {
                SpellField<?> field = spell.getSpell().getField(fieldName);
                String fieldSelector = selector + "#Parent #FieldsGroup[" + index.getAndIncrement() + "] ";

                if (field.isBoolean()) {
                    ui.append(selector + "#Parent #FieldsGroup", LAYOUT_FIELD_BOOLEAN);
                    ui.set(fieldSelector + "#Input.Value", (boolean) field.getValue(spell));
                } else if (field.isEnum()) {
                    ui.append(selector + "#Parent #FieldsGroup", LAYOUT_FIELD_ENUM);
                    this.buildEnum(spell, field, ui, events, fieldSelector);
                } else {
                    ui.append(selector + "#Parent #FieldsGroup", LAYOUT_FIELD_STRING);
                    ui.set(fieldSelector + "#Input.PlaceholderText", fieldName);
                    ui.set(fieldSelector + "#Input.Value", field.asString(spell));
                }

                EventData data = EventData
                        .of("ACTION", "spellFieldChange")
                        .put("SPELL", uuid)
                        .put("META", fieldName)
                        .put("SELECTOR", fieldSelector);
                if (!field.isBoolean())
                    data.put("@VALUE", fieldSelector + "#Input.Value");
                events.addEventBinding(CustomUIEventBindingType.ValueChanged, fieldSelector + "#Input", data);
                ui.set(fieldSelector + "#FieldLabel.Text", fieldName);
                ui.set(fieldSelector + "#FieldLabel.Style", VALUE_FIELD_LABEL_STYLE);
            });
        }

        if (this.newCategory != null) {
            ui.set(selector + "#Parent #NavGroup #SaveButton.Disabled", true);
            ui.set(selector + "#Parent #NavGroup #RemoveButton.Disabled", true);
            ui.set(selector + "#Parent #NavGroup #CancelButton.Disabled", true);
            ui.set(selector + "#Parent #NavGroup #AddChildButton.Disabled", true);
        } else {
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #NavGroup #SaveButton", EventData
                    .of("ACTION", "spellEdit")
                    .put("SPELL", uuid)
                    .put("META", "Save")
            );
            if (spell.getChildren().length > 0 || spell.getParent() == null) {
                ui.set(selector + "#Parent #NavGroup #RemoveButton.Disabled", true);
            } else {
                events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #NavGroup #RemoveButton", EventData
                        .of("ACTION", "spellEdit")
                        .put("SPELL", uuid)
                        .put("META", "Remove")
                );
            }
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #NavGroup #CancelButton", EventData
                    .of("ACTION", "spellEdit")
                    .put("SPELL", uuid)
                    .put("META", "Cancel")
            );
            events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #NavGroup #AddChildButton", EventData
                    .of("ACTION", "spellEdit")
                    .put("SPELL", uuid)
                    .put("SELECTOR", selector)
                    .put("META", "AddChild")
            );
        }
    }
    private void buildEnum(SpellContext spell, SpellField<?> field, UICommandBuilder ui, UIEventBuilder events, String selector) {
        try {
            this.buildEnum0(spell, field, ui, events, selector);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    private void buildEnum0(SpellContext spell, SpellField<?> field, UICommandBuilder ui, UIEventBuilder events, String selector) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> enumClass = this.getEnumClass(field);

        List<DropdownEntryInfo> entries = new ArrayList<>();
        Enum<?>[] values = Adapter.cast(enumClass.getMethod("values").invoke(null));
        for (Enum<?> en : values) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(en.toString()), en.name()));
        }

        Enum<?> value = (Enum<?>) field.getValue(spell);
        if (value == null) {
            value = values[0];
            spell.getFieldsData().put(field.name(), field.codec().encode(Adapter.cast(value)));
        }
        ui.set(selector + "#Input.Entries", entries);
        ui.set(selector + "#Input.Value", value.name());
    }
    private Class<Enum<?>> getEnumClass(SpellField<?> field) throws NoSuchFieldException, IllegalAccessException {
        Field clazz = EnumCodec.class.getDeclaredField("clazz");
        clazz.setAccessible(true);
        return Adapter.cast(clazz.get(field.codec()));
    }

    private void handleDataEventSpellEdit(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Category newCategory;
        switch (data.meta) {
            case "Cancel":
                this.openCategory(ref, store, ui, events, super.category);
                this.oldSpellCategory = null;
                this.spell = null;
                break;
            case "Save":
                if (!this.spell.validate()) {
                    sendUpdate();
                    this.playerRef.sendMessage(Message.raw("Invalid spell data"));
                    return;
                }

                newCategory = this.copyFromRoot(
                        this.spell.getCategory(),
                        old -> old.getUuid().equals(this.spell.getUuid()) ? this.spell : old
                );

                try {
                    Data.replaceCategory(this.spell.getCategory(), newCategory);
                    HySpellEnginePlugin.debugLog("Save: replacing spell.category");
                } catch (IndexOutOfBoundsException e) {
                    Data.replaceCategory(this.oldSpellCategory, newCategory);
                    this.oldSpellCategory = null;
                    HySpellEnginePlugin.debugLog("Save: replacing oldSpellCategory");
                }
                this.spell = null;
                this.openCategory(ref, store, ui, events, newCategory);
                break;
            case "AddChild":
                SpellContext[] children = new SpellContext[this.spell.getChildren().length + 1];
                int i = 0;
                for (SpellContext child : this.spell.getChildren())
                    children[i++] = child;

                 SpellContext newSpell = new SpellContext(
                        Spell.getSpellRegistry().getSpell("command"),
                        new SpellContext.Display("", "", Path.of("")),
                        UUID.randomUUID(),
                        BsonDocument.parse("{\"command\":\"help\",\"repeatable\":false}"),
                        new SpellContext[0]
                );
                children[children.length - 1] = newSpell;
                this.spell = super.category.getSpell(this.spell.getUuid());
                Category category = copyFromRoot(this.spell.getCategory(), old -> old.getUuid().equals(this.spell.getUuid()) ? new SpellContext(
                        this.spell.getSpell(),
                        this.spell.getDisplay(),
                        this.spell.getUuid(),
                        this.spell.getFieldsData(),
                        children
                ) : old);
                this.setUnaccessible(newSpell, this.spell.getParent(), category);
                this.oldSpellCategory = this.spell.getCategory();
                this.openCategory(ref, store, category);
                super.category = this.oldSpellCategory;
                String selector = data.selector + "#Children[" + (children.length-1) + "] ";
                this.openSpellEdit(ref, store, newSpell, selector, ui, events);
                ui.set(selector + "#Parent #NavGroup #AddChildButton.Disabled", true);
                ui.set(selector + "#Parent #NavGroup #RemoveButton.Disabled", true);
                break;
            case "Remove":
                if (this.spell.getChildren().length > 0 || this.spell.getParent() == null)
                    break;

                SpellContext parent = this.spell.getParent();
                SpellContext[] oldChildren = parent.getChildren();
                SpellContext[] newChildren = new SpellContext[oldChildren.length - 1];
                int index = 0;
                for (SpellContext oldChild : oldChildren) {
                    if (oldChild.getUuid().equals(this.spell.getUuid()))
                        continue;
                    newChildren[index++] = oldChild;
                }

                newCategory = this.copyFromRoot(
                        this.spell.getCategory(),
                        old -> old.getUuid().equals(parent.getUuid()) ? old : new SpellContext(
                                parent.getSpell(),
                                parent.getDisplay(),
                                parent.getUuid(),
                                parent.getFieldsData(),
                                newChildren
                        )
                );
                Data.replaceCategory(this.spell.getCategory(), newCategory);
                this.spell = null;
                this.openCategory(ref, store, ui, events, newCategory);
                break;
        }

        sendUpdate(ui, events, false);
    }
    private void handleDataEventSpellTypeChange(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        Spell spellType = Spell.getSpellRegistry().getSpell(data.value);
        SpellContext spell = this.setUnaccessible(new SpellContext(
                spellType,
                this.spell.getDisplay(),
                this.spell.getUuid(),
                new BsonDocument(),
                this.spell.getChildren()
        ), this.spell.getParent(), this.spell.getCategory());

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        this.spell = null;
        this.openSpellEdit(ref, store, spell, data.selector, ui, events);

        sendUpdate(ui, events, false);
    }
    private void handleDataEventSpellDisplayChange(SpellsUIEventData data) {
        SpellContext.Display display = this.spell.getDisplay();
        switch (data.meta) {
            case "Name" -> display = new SpellContext.Display(
                    data.value,
                    display.description(),
                    display.icon()
            );
            case "Desc" -> display = new SpellContext.Display(
                    display.name(),
                    data.value,
                    display.icon()
            );
            case "Icon" -> display = new SpellContext.Display(
                    display.name(),
                    display.description(),
                    Path.of(data.value.length() > 3 ? data.value : "") // Assets path <= 3 -> client crash
            );
        }

        SpellContext spell = new SpellContext(
                this.spell.getSpell(),
                display,
                this.spell.getUuid(),
                this.spell.getFieldsData(),
                this.spell.getChildren()
        );
        this.setUnaccessible(spell, this.spell.getParent(), this.spell.getCategory());
        this.spell = spell;

        HySpellEnginePlugin.debugLog(spell);

        sendUpdate();
    }
    private <T> void handleDataEventSpellFieldChange(SpellsUIEventData data) {
        SpellField<T> field = this.spell.getSpell().getField(data.meta);
        UICommandBuilder ui = new UICommandBuilder();
        T value = null;

        if (field.isBoolean()) {
            value = Adapter.cast(!(boolean) field.getValue(this.spell));
        } else if (field.isEnum()) {
            try {
                value = field.fromString(data.value);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            String color;
            try {
                value = field.fromString(data.value);
                color = "#ffffff";
            } catch (Throwable e) {
                color = "#FF3300";
            }

            ui.set(data.selector + "#Input.Style.TextColor", color);
        }

        if (value != null) {
            this.spell.getFieldsData().put(field.name(), field.codec().encode(value));
        }

        sendUpdate(ui);
    }

    private Category copyFromRoot(Category category, Function<SpellContext, SpellContext> replacer) {
        return new Category(
                category.display(),
                category.experience(),
                copyFromRoot(replacer.apply(category.root().clone()), replacer)
        );
    }
    private SpellContext copyFromRoot(SpellContext spell, Function<SpellContext, SpellContext> replacer) {
        List<SpellContext> children = new ArrayList<>();
        for (SpellContext child : spell.getChildren()) {
            SpellContext newChild = replacer.apply(child.clone());
            if (newChild != null)
                children.add(this.copyFromRoot(newChild, replacer));
        }
        return new SpellContext(
                spell.getSpell(),
                spell.getDisplay(),
                spell.getUuid(),
                spell.getFieldsData(),
                children.stream().filter(Objects::nonNull).toArray(SpellContext[]::new)
        );
    }

    private SpellContext setUnaccessible(SpellContext spell, SpellContext parent, Category category) {
        // Is that really a good way?
        try {
            Field parentField = SpellContext.class.getDeclaredField("parent");
            Field categoryField = SpellContext.class.getDeclaredField("category");
            parentField.setAccessible(true);
            categoryField.setAccessible(true);
            parentField.set(spell, parent);
            categoryField.set(spell, category);
            return spell;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
