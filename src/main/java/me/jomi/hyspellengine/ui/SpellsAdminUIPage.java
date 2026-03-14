package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
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
import me.jomi.hyspellengine.utils.UIBuilder;
import org.bson.BsonDocument;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// TODO Doubles with .0 display as Int

// TODO bugfix - after add category -> change any category value -> cancel = opened nonexisting category
public class SpellsAdminUIPage extends SpellsUIPage {
    public static final String LAYOUT_SPELL = "HySpellEngine/Spells/Admin/Spell.ui";
    public static final String LAYOUT_FIELDS = "HySpellEngine/Spells/Admin/Fields.ui";
    public static final String LAYOUT_CATEGORY = "HySpellEngine/Spells/Admin/Category.ui";
    public static final String LAYOUT_FIELD_ENUM = "HySpellEngine/Spells/Admin/EnumField.ui";
    public static final String LAYOUT_FIELD_STRING = "HySpellEngine/Spells/Admin/StringField.ui";
    public static final String LAYOUT_FIELD_BOOLEAN = "HySpellEngine/Spells/Admin/BooleanField.ui";
    public static final String LAYOUT_ADD_CATEGORY = "HySpellEngine/Spells/Admin/AddCategoryButton.ui";

    public static final Value<?> VALUE_FIELD_LABEL_STYLE = Value.ref(LAYOUT_FIELDS, "FieldLabelStyle");
    public static final Value<?> VALUE_FIELD_TEXT_TOOLTIP_STYLE = Value.ref("Common.ui", "DefaultTextTooltipStyle");

    private int editedCategoryIndex = -1;
    private Category editedCategory;
    private Category newCategory = null;
    private Category oldSpellCategory;
    private SpellContext spell;
    private boolean newSpell = false;


    public SpellsAdminUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }


    @Override
    protected void openExperiences() {
        Ref<EntityStore> ref = this.playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new ExperienceAdminUIPage(this.playerRef));
    }

    @Override
    protected void openCategory(Category category) {
        if (this.newCategory == null || category == this.newCategory) {
            super.openCategory(category);
            this.spell = null;
        }
    }
    @Override
    protected void addCategory(UIBuilder ui, Category category, int index) {
        super.addCategory(ui, category, index);
        ui.onClickRight("#CategoryButton", "openEditCategory", "CATEGORY", String.valueOf(index));
    }

    @Override
    protected void openSpells() {
        super.openSpells();

        if (this.newCategory == null) {
            ui.append("#Categories", LAYOUT_ADD_CATEGORY);
            ui.onClick("#Categories #AddCategoryButtonRoot #Button", "addCategory");
        } else {
            ui.append("#Categories", SpellsUIPage.LAYOUT_CATEGORY); // closeEditedCategory other usage
            int index = Data.getCategories().length;
            this.addCategory(ui.at("#Categories", index), this.newCategory, index);
        }
    }
    @Override
    protected void addSpell(SpellContext spell, UIBuilder ui) {
        super.addSpell(spell, ui);

        ui.set("#Parent #SpellButton.Disabled", false);
        ui.onClickRight("#Parent #SpellButton", "spell", "SPELL", spell.getUuid().toString(), "SELECTOR", ui.selector());
    }

    @Override
    protected void handleDataEventSpell(SpellsUIEventData data) {
        SpellContext spell = super.category.getSpell(UUID.fromString(data.spell));
        this.openSpellEdit(spell, ui.at(data.selector));
    }


    private void makeDisplay(UIBuilder ui, String action, String name, String desc, Path icon, String... meta) {
        meta = Arrays.copyOf(meta, meta.length + 2);
        meta[meta.length - 2] = "META";

        ui.append("#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set("#DisplayGroup[0] #FieldLabel.Text", "Name");
        ui.set("#DisplayGroup[0] #Input.PlaceholderText", "Name");
        ui.set("#DisplayGroup[0] #Input.Value", name);
        meta[meta.length - 1] = "Name";
        ui.onChange("#DisplayGroup[0] #Input", action, meta);

        ui.append("#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set("#DisplayGroup[1] #FieldLabel.Text", "Desc");
        ui.set("#DisplayGroup[1] #Input.PlaceholderText", "Description");
        ui.set("#DisplayGroup[1] #Input.Value", desc);
        meta[meta.length - 1] = "Desc";
        ui.onChange("#DisplayGroup[1] #Input", action, meta);

        ui.append("#DisplayGroup", LAYOUT_FIELD_STRING);
        ui.set("#DisplayGroup[2] #FieldLabel.Text", "Icon");
        ui.set("#DisplayGroup[2] #Input.PlaceholderText", "Icon");
        ui.set("#DisplayGroup[2] #Input.Value", icon.toString().replace('\\', '/'));
        meta[meta.length - 1] = "Icon";
        ui.onChange("#DisplayGroup[2] #Input", action, meta);
    }


    private void closeEditedCategory() {
        this.doWithOther(() -> {
            if (this.editedCategoryIndex == -1 || this.editedCategoryIndex >= Data.getCategories().length || this.newCategory != null) {
                if (super.category == this.newCategory)
                    super.category = null;
                this.newCategory = null;
                this.openSpells();
            } else {
                UIBuilder ui = this.ui.at("#Categories", this.editedCategoryIndex);
                ui.clear("");
                ui.append("", SpellsUIPage.LAYOUT_CATEGORY);
                this.addCategory(ui, Data.getCategories()[this.editedCategoryIndex], this.editedCategoryIndex);

                this.editedCategory = null;
                this.editedCategoryIndex = -1;
            }
        });
    }
    private void openEditCategory(Category category, UIBuilder ui) {
        if (this.newCategory != null && category != this.newCategory)
            closeEditedCategory();

        ui.clear("");
        ui.append("", LAYOUT_CATEGORY);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getExperienceRegistry().forEach((id, exp) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set("#ExperienceGroup #ExperienceInput.Value", category.experience().getName());
        ui.set("#ExperienceGroup #ExperienceInput.Entries", values);
        ui.onChange("#ExperienceGroup #ExperienceInput", "categoryChange", "META", "exp");

        this.makeDisplay(
                ui,
                "categoryChange",
                category.display().name(),
                category.display().description(),
                category.display().icon()
        );

        UIBuilder uiNav = ui.at("#NavGroup");
        uiNav.onClick("#CancelButton", "categoryEdit", "META", "Cancel");
        uiNav.onClick("#RemoveButton", "categoryEdit", "META", "Remove");
        uiNav.onClick("#SaveButton", "categoryEdit", "META", "Save");

        if (Data.getCategories().length == 1)
            uiNav.set("#RemoveButton.Disabled", true);
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

    private void handleDataEventOpenEditCategory(SpellsUIEventData data) {
        if (this.editedCategory != null)
            this.closeEditedCategory();

        this.editedCategoryIndex = Integer.parseInt(data.category);
        Category category = Data.getCategories()[this.editedCategoryIndex];
        this.editedCategory = category;

        this.openEditCategory(category, ui.at("#Categories", this.editedCategoryIndex));
    }
    private void handleDataEventCategoryEdit(SpellsUIEventData data) {
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
                            this.spell,
                            this.newCategory.uuid()
                    );
                    if (!this.validateNewCategory()) {
                        playerRef.sendMessage(Message.raw("Invalid data in category or root spell"));
                        return;
                    }
                    Data.addCategory(this.newCategory);
                    super.category = this.newCategory;
                    this.newCategory = null;
                    this.editedCategoryIndex = -1;

                } else
                    throw new RuntimeException("Unknown category to save");
                break;
            case "Remove":
                Data.removeCategory(Data.getCategories()[this.editedCategoryIndex]);

                if (super.category != null && super.category.root().equals(this.editedCategory.root()))
                    super.category = null;

                this.openSpells();

                return;
        }

        this.closeEditedCategory();
    }
    private void handleDataEventCategoryChangeField(SpellsUIEventData data) {
        switch (data.meta) {
            case "Name" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                data.value,
                                this.editedCategory.display().description(),
                                this.editedCategory.display().icon()
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root(),
                            this.editedCategory.uuid()
                    );
            case "Desc" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                this.editedCategory.display().name(),
                                data.value,
                                this.editedCategory.display().icon()
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root(),
                            this.editedCategory.uuid()
                    );
            case "Icon" ->
                    this.editedCategory = new Category(
                            new Category.Display(
                                this.editedCategory.display().name(),
                                this.editedCategory.display().description(),
                                Path.of(data.value.length() > 3 ? data.value : "")
                            ),
                            this.editedCategory.experience(),
                            this.editedCategory.root(),
                            this.editedCategory.uuid()
                    );
            case "exp" ->
                    this.editedCategory = new Category(
                            this.editedCategory.display(),
                            Experience.getRegistry().getExperience(data.value),
                            this.editedCategory.root(),
                            this.editedCategory.uuid()
                    );

        }

        if (this.newCategory != null)
            this.newCategory = this.editedCategory;
    }
    private void handleDataEventAddCategory(SpellsUIEventData data) {
        Path emptyPath = Path.of("");
        this.newCategory = new Category(
                new Category.Display("", "", emptyPath),
                Experience.getRegistry().getExperience(Experience.getRegistry().getKeys().stream().findAny().get()),
                new SpellContext(
                        Spell.getSpellRegistry().getSpell(Spell.getSpellRegistry().getKeys().stream().findAny().get()),
                        new SpellContext.Display("", "", emptyPath),
                        UUID.randomUUID(),
                        new BsonDocument(),
                        new SpellContext[0]
                ),
                UUID.randomUUID()
        );

        this.doWithOther(() -> {
            //this.addCategory(ui, events, this.newCategory, Data.getCategories().length);
            //this.openCategory(ref, store, ui, events, this.newCategory);
            super.category = this.newCategory;
            this.editedCategory = this.newCategory;
            this.openSpells();
        });


        UIBuilder ui = this.ui.at("#Categories", Data.getCategories().length);

        this.openEditCategory(category, ui);
        ui.set("#RemoveButton.Disabled", true);

        this.openSpellEdit(this.newCategory.root(), this.ui.at("#Spells"));
    }


    protected void openSpellEdit(SpellContext spell, UIBuilder rootUI) {
        if (this.spell != null) {
            // clear previous edit
            this.doWithOther(() -> this.openCategory(super.category));
            this.newSpell = false;
        }

        UIBuilder ui = rootUI.at("#Parent");

        this.spell = spell.clone();
        String uuid = spell.getUuid().toString();

        ui.clear();
        ui.append(LAYOUT_SPELL);

        List<DropdownEntryInfo> values = new ArrayList<>();
        HySpellEnginePlugin.getInstance().getSpellRegistry().forEach((id, s) -> {
            values.add(new DropdownEntryInfo(LocalizableString.fromString(id), id));
        });

        ui.set("#SpellTypeInput.Value", spell.getSpell().getName());
        ui.set("#SpellTypeInput.Entries", values);
        ui.set("#SpellTypeInput.TextTooltipStyle", VALUE_FIELD_TEXT_TOOLTIP_STYLE);
        ui.set("#SpellTypeInput.TooltipText", spell.getSpell().getDescription());
        ui.onChange("#SpellTypeInput", "spellTypeChange", "SPELL", uuid, "SELECTOR", rootUI.selector());

        this.makeDisplay(
                ui,
                "spellDisplayChange",
                spell.getDisplay().name(),
                spell.getDisplay().description(),
                spell.getDisplay().icon(),
                "SPELL", uuid
        );

        if (spell.getFieldsData() != null) {
            AtomicInteger index = new AtomicInteger(0);
            spell.getSpell().getFieldsKeys().stream().sorted().forEach(fieldName -> {
                SpellField<?> field = spell.getSpell().getField(fieldName);
                UIBuilder uiField = ui.at("#FieldsGroup", index.getAndIncrement());

                if (field.isBoolean()) {
                    ui.append("#FieldsGroup", LAYOUT_FIELD_BOOLEAN);
                    uiField.set("#Input.Value", (boolean) field.getValue(spell));
                } else if (field.isEnum()) {
                    ui.append("#FieldsGroup", LAYOUT_FIELD_ENUM);
                    this.buildEnum(spell, field, uiField);
                } else {
                    ui.append("#FieldsGroup", LAYOUT_FIELD_STRING);
                    uiField.set("#Input.PlaceholderText", fieldName);
                    uiField.set("#Input.Value", field.asString(spell));
                }

                TriConsumer<String, String, String[]> on = field.isBoolean() ? uiField::onCheckBox : uiField::onChange;
                on.accept("#Input", "spellFieldChange", new String[]{"SPELL", uuid, "META", fieldName, "SELECTOR", uiField.selector()});

                uiField.set("#FieldLabel.Text", fieldName);
                uiField.set("#FieldLabel.Style", VALUE_FIELD_LABEL_STYLE);
                uiField.set(".TextTooltipStyle", VALUE_FIELD_TEXT_TOOLTIP_STYLE);
                uiField.set(".TooltipText", field.description());
            });
        }

        UIBuilder uiNav = ui.at("#NavGroup");
        if (this.newCategory != null) {
            uiNav.set("#SaveButton.Disabled", true);
            uiNav.set("#RemoveButton.Disabled", true);
            uiNav.set("#CancelButton.Disabled", true);
            uiNav.set("#AddChildButton.Disabled", true);
        } else {
            uiNav.onClick("#SaveButton", "spellSave", "SPELL", uuid);
            uiNav.onClick("#RemoveButton", "spellRemove", "SPELL", uuid);
            uiNav.onClick("#CancelButton", "spellCancel", "SPELL", uuid);
            uiNav.onClick("#AddChildButton", "spellAddChild", "SPELL", uuid, "SELECTOR", rootUI.selector());
        }

        if (spell.getChildren().length > 0 || spell.getParent() == null)
            uiNav.set("#RemoveButton.Disabled", true);
        if (this.newSpell) {
            uiNav.set("#AddChildButton.Disabled", true);
            uiNav.set("#RemoveButton.Disabled", true);
        }
    }
    private void buildEnum(SpellContext spell, SpellField<?> field, UIBuilder ui) {
        try {
            this.buildEnum0(spell, field, ui);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    private void buildEnum0(SpellContext spell, SpellField<?> field, UIBuilder ui) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
        ui.set("#Input.Entries", entries);
        ui.set("#Input.Value", value.name());
    }
    private Class<Enum<?>> getEnumClass(SpellField<?> field) throws NoSuchFieldException, IllegalAccessException {
        Field clazz = EnumCodec.class.getDeclaredField("clazz");
        clazz.setAccessible(true);
        return Adapter.cast(clazz.get(field.codec()));
    }

    private void handleDataEventSpellCancel(SpellsUIEventData data) {
        this.openCategory(super.category);
        this.oldSpellCategory = null;
        this.spell = null;
    }
    private void handleDataEventSpellSave(SpellsUIEventData data) {
        if (!this.spell.validate()) {
            this.playerRef.sendMessage(Message.raw("Invalid spell data"));
            return;
        }

        Category newCategory = this.copyFromRoot(this.spell.getCategory(), this.spell);

        try {
            Data.replaceCategory(this.spell.getCategory(), newCategory);
        } catch (IndexOutOfBoundsException e) {
            Data.replaceCategory(this.oldSpellCategory, newCategory);
            this.oldSpellCategory = null;
        }
        this.spell = null;
        this.newSpell = false;
        this.openCategory(newCategory);
    }
    private void handleDataEventSpellAddChild(SpellsUIEventData data) {
        SpellContext[] children = Arrays.copyOf(this.spell.getChildren(), this.spell.getChildren().length + 1);

        SpellContext newSpell = new SpellContext(
                Spell.getSpellRegistry().getSpell("command"),
                new SpellContext.Display("", "", Path.of("")),
                UUID.randomUUID(),
                BsonDocument.parse("{\"command\":\"help\",\"repeatable\":false}"),
                new SpellContext[0]
        );
        children[children.length - 1] = newSpell;

        this.spell = super.category.getSpell(this.spell.getUuid()); // ensure non modified
        Category category = copyFromRoot(this.spell.getCategory(), new SpellContext(
                this.spell.getSpell(),
                this.spell.getDisplay(),
                this.spell.getUuid(),
                this.spell.getFieldsData(),
                children
        ));
        this.setUnaccessible(newSpell, this.spell.getParent(), category);

        this.oldSpellCategory = this.spell.getCategory();
        this.doWithOther(() -> this.openCategory(category));
        super.category = this.oldSpellCategory;

        this.newSpell = true;
        this.openSpellEdit(newSpell, this.ui.at(data.selector + "#Children", children.length-1));
    }
    private void handleDataEventSpellRemove(SpellsUIEventData data) {
        if (this.spell.getChildren().length > 0 || this.spell.getParent() == null)
            return;

        SpellContext parent = this.spell.getParent();
        SpellContext[] oldChildren = parent.getChildren();
        SpellContext[] newChildren = new SpellContext[oldChildren.length - 1];
        int index = 0;
        for (SpellContext oldChild : oldChildren) {
            if (oldChild.getUuid().equals(this.spell.getUuid()))
                continue;
            newChildren[index++] = oldChild;
        }

        newCategory = this.copyFromRoot(this.spell.getCategory(), new SpellContext(
                parent.getSpell(),
                parent.getDisplay(),
                parent.getUuid(),
                parent.getFieldsData(),
                newChildren
        ));
        Data.replaceCategory(this.spell.getCategory(), newCategory);
        this.spell = null;
        this.openCategory(newCategory);
    }

    private void handleDataEventSpellTypeChange(SpellsUIEventData data) {
        Spell spellType = Spell.getSpellRegistry().getSpell(data.value);
        SpellContext spell = this.setUnaccessible(new SpellContext(
                spellType,
                this.spell.getDisplay(),
                this.spell.getUuid(),
                new BsonDocument(),
                this.spell.getChildren()
        ), this.spell.getParent(), this.spell.getCategory());

        this.spell = null;
        this.openSpellEdit(spell, ui.at(data.selector));
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
    }
    private <T> void handleDataEventSpellFieldChange(SpellsUIEventData data) {
        SpellField<T> field = this.spell.getSpell().getField(data.meta);
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

            ui.at(data.selector).set("#Input.Style.TextColor", color);
        }

        if (value != null)
            this.spell.getFieldsData().put(field.name(), field.codec().encode(value));
    }


    private final Set<UUID> copied = new HashSet<>();
    private Category copyFromRoot(Category category, SpellContext newSpell) {
        Function<SpellContext, SpellContext> replacer = spell -> spell.getUuid().equals(newSpell.getUuid()) ? newSpell : spell;

        this.copied.clear();
        return new Category(
                category.display(),
                category.experience(),
                copyFromRoot(replacer.apply(category.root().clone()), replacer),
                category.uuid()
        );
    }
    private SpellContext copyFromRoot(SpellContext spell, Function<SpellContext, SpellContext> replacer) {
        if (!copied.add(spell.getUuid())) {
            throw new RuntimeException("spells must be unique");
        }

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


    static {
        bindEvent(SpellsAdminUIPage.class, "openEditCategory", SpellsAdminUIPage::handleDataEventOpenEditCategory);
        bindEvent(SpellsAdminUIPage.class, "categoryChange", SpellsAdminUIPage::handleDataEventCategoryChangeField);
        bindEvent(SpellsAdminUIPage.class, "categoryEdit", SpellsAdminUIPage::handleDataEventCategoryEdit);
        bindEvent(SpellsAdminUIPage.class, "addCategory", SpellsAdminUIPage::handleDataEventAddCategory);


        bindEvent(SpellsAdminUIPage.class, "spellSave", SpellsAdminUIPage::handleDataEventSpellSave);
        bindEvent(SpellsAdminUIPage.class, "spellRemove", SpellsAdminUIPage::handleDataEventSpellRemove);
        bindEvent(SpellsAdminUIPage.class, "spellCancel", SpellsAdminUIPage::handleDataEventSpellCancel);
        bindEvent(SpellsAdminUIPage.class, "spellAddChild", SpellsAdminUIPage::handleDataEventSpellAddChild);

        bindEvent(SpellsAdminUIPage.class, "spellTypeChange", SpellsAdminUIPage::handleDataEventSpellTypeChange);
        bindEvent(SpellsAdminUIPage.class, "spellDisplayChange", SpellsAdminUIPage::handleDataEventSpellDisplayChange);
        bindEvent(SpellsAdminUIPage.class, "spellFieldChange", SpellsAdminUIPage::handleDataEventSpellFieldChange);
    }
}
