package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// TODO init inf exp = 0 ? red : not red
public class ExperienceAdminUIPage extends SpellsUIPage {
    private class ExpValue {
        String node = null;
        String key;
        double exp;
        ExpValue(String key, double exp) {
            this.key = key;
            this.exp = exp;

            if (this.isKeyValid())
                this.node = this.key;
        }

        static String selector(int index) {
            return "#ExperienceValuesRoot #Values[" + index + "] ";
        }

        boolean isValid() {
            return this.isValueValid() && this.isKeyValid();
        }
        boolean isValueValid() {
            return this.exp != 0;
        }
        boolean isKeyValid() {
            if (this.key.isBlank())
                return false;

            for (ExpValue value : ExperienceAdminUIPage.this.values)
                if (this != value && value.key.equals(this.key))
                    return false;

            return true;
        }

        void trySave() {
            if (this.isValid())
                this.save();
        }
        void save() {
            if (this.node != null)
                Data.set(ExperienceAdminUIPage.this.experience, this.node, 0);
            Data.set(ExperienceAdminUIPage.this.experience, this.key, this.exp);
            this.node = this.key;
        }
    }

    public ExperienceAdminUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }

    public static final String LAYOUT_EXPERIENCE = ""; // TODO
    public static final String LAYOUT_EXPERIENCE_VALUE = "HySpellEngine/Spells/Admin/ExperienceValue.ui";
    public static final String LAYOUT_EXPERIENCE_VALUES = "HySpellEngine/Spells/Admin/ExperienceValues.ui";
    public static final String LAYOUT_EXPERIENCE_LEVEL = "HySpellEngine/Spells/Admin/ExperienceLevel.ui";
    public static final String LAYOUT_EXPERIENCE_LEVELS = "HySpellEngine/Spells/Admin/ExperienceLevels.ui";
    public static final String LAYOUT_EXPERIENCE_EDIT_BUTTON = "HySpellEngine/Spells/Admin/ExperienceEditButton.ui";

    protected Experience experience;
    protected final List<ExpValue> values = new ArrayList<>();


    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        HySpellEnginePlugin.debugLog("building experiences admin");
        super.build(ref, ui, events, store);
        this.openExperiences(ref, store, ui, events);
    }

    private boolean firstOpen = true;
    @Override
    protected void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        if (this.firstOpen) {
            HySpellEnginePlugin.debugLog("exp spells first open canceled");
            this.firstOpen = false;
            return;
        }
        HySpellEnginePlugin.debugLog("exp spells  open ");
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new SpellsAdminUIPage(this.playerRef));
    }
    @Override
    protected void openExperiences(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        ui.clear("#Container");
        ui.append("#Container", SpellsUIPage.LAYOUT_EXPERIENCE_MAIN);

        this.experience = null;

        AtomicInteger i = new AtomicInteger();
        Experience.getRegistry().forEach((id, experience) -> {
            ui.append("#Experiences", SpellsUIPage.LAYOUT_EXPERIENCE);
            String selector = "#Experiences[" + i.getAndIncrement() + "] ";
            this.openExperience(ref, store, ui, events, experience, selector);
        });
    }

    @Override
    protected void openExperience(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Experience experience, String selector) {
        super.openExperience(ref, store, ui, events, experience, selector);

        HySpellEnginePlugin.debugLog("opening exp " + experience.getName());

        ui.append(selector.trim(), LAYOUT_EXPERIENCE_EDIT_BUTTON);

        for (CustomUIEventBindingType type : new CustomUIEventBindingType[]{CustomUIEventBindingType.Activating, CustomUIEventBindingType.RightClicking})
            events.addEventBinding(type, selector + "#ExperienceEditButton", EventData
                    .of("ACTION", "openExperienceEdit")
                    .put("EXPERIENCE", experience.getName())
                    .put("SELECTOR", selector)
            );
    }

    // TODO remove value button
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.action) {
            case "openExperienceEdit" -> this.handleDataEventOpenEditExperience(ref, store, data);

            case "addValue" -> this.handleDataEventAddValue(data);
            case "valueChangeKey" -> this.handleDataEventValueChangeKey(data);
            case "valueChangeValue" -> this.handleDataEventValueChangeValue(data);
            case "valueRemove" -> this.handleDataEventValueRemove(data);

            case "addLevel" -> this.handleDataEventAddLevel(data);
            case "levelChange" -> this.handleDataEventLevelChange(data);
            case "levelRemove" -> this.handleDataEventLevelRemove(data);

            case "swapInfinity" -> this.handleDataEventSwapInfinity(data);
            case "editInfinityExp" -> this.handleDataEventEditInfinityExp(data);

            default -> super.handleDataEvent(ref, store, data);
        }
    }


    private void handleDataEventValueRemove(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        int index = Integer.parseInt(data.selector);
        ExpValue value = this.values.get(index);

        if (value.node != null) {
            Data.set(this.experience, value.node, 0);
        }

        // ui.remove(ExpValue.selector(index).trim());
        // removing requires to reindexing each event, hiding not
        ui.set(ExpValue.selector(index).trim() + ".Visible", false);


        sendUpdate(ui, events, false);
    }
    private void handleDataEventValueChangeKey(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        int index = Integer.parseInt(data.selector);
        ExpValue value = this.values.get(index);

        String previousKey = value.key;
        value.key = data.value.trim();

        for (int i = 0; i < this.values.size(); i++) {
            ExpValue v = this.values.get(i);
            if (v.key.equals(previousKey) || v.key.equals(value.key)) {
                if (v.key.equals(previousKey))
                    v.trySave();
                ui.set(ExpValue.selector(i) + "#InputKey.Style.TextColor", v.isKeyValid() ? "#009900" : "#FF3300");
            }
        }

        if (value.isKeyValid()) {
            value.trySave();
        }

        sendUpdate(ui, events, false);
    }
    private void handleDataEventValueChangeValue(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        int index = Integer.parseInt(data.selector);
        ExpValue value = this.values.get(index);

        String color;
        try {
            value.exp = Double.parseDouble(data.value);
            if (!value.isValueValid())
                throw new NumberFormatException();
            color = "#FF9900";
        } catch (NumberFormatException e) {
            color = "#FF3300";
            value.exp = 0;
        }

        String selector = ExpValue.selector(index);
        ui.set(selector + "#InputValue.Style.TextColor", color);

        value.trySave();

        sendUpdate(ui, events, false);
    }
    private void handleDataEventAddValue(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        this.addValue(ui, events, "", 0);

        sendUpdate(ui, events, false);
    }

    private void handleDataEventLevelRemove(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Experience.Level[] oldLevels = this.experience.getLevels();
        Experience.Level[] newLevels = new Experience.Level[oldLevels.length - 1];
        int index = Integer.parseInt(data.selector);

        int oldI = -1;
        int newI = 0;
        for (Experience.Level level : oldLevels) {
            if (++oldI == index)
                continue;
            newLevels[newI++] = level;
        }

        Data.set(this.experience, newLevels);


        Experience experience = this.experience;
        this.experience = null;

        this.openExperiences(this.playerRef.getReference(), this.playerRef.getReference().getStore(), ui, events);
        this.openEditExperience(ui, events, experience);

        sendUpdate(ui, events, false);
    }
    private void handleDataEventLevelChange(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        int index = Integer.parseInt(data.selector);
        Experience.Level[] levels = this.experience.getLevels();
        Experience.Level level = levels[index];
        String selector = "#ExperienceLevelsRoot #Levels[" + data.selector + "] ";

        switch (data.meta) {
            case "exp":
                double exp;
                try {
                    exp = Double.parseDouble(data.value);
                } catch (NumberFormatException e) {
                    exp = 0;
                }
                ui.set(selector + "#InputExperience.Style.TextColor", exp <= 0 ? "#FF3300" : "#FFFFFF");

                if (exp <= 0) {
                    sendUpdate(ui, events, false);
                    return;
                }

                levels[index] = new Experience.Level(
                        index > 0 ? levels[index - 1].exp() + exp : exp,
                        level.infinite(),
                        level.chatMessage(),
                        level.sound()
                );
                break;
            case "sound":
                levels[index] = new Experience.Level(
                        level.exp(),
                        level.infinite(),
                        level.chatMessage(),
                        data.value
                );
                break;
            case "msg":
                levels[index] = new Experience.Level(
                        level.exp(),
                        level.infinite(),
                        data.value,
                        level.sound()
                );
                break;
        }

        Data.set(this.experience, levels);

        sendUpdate(ui, events, false);
    }
    private void handleDataEventAddLevel(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Experience.Level[] levels = this.experience.getLevels();
        levels = Arrays.copyOf(levels, levels.length + 1);
        int index = levels.length - 1;
        // move infinity level to last index
        if (levels.length > 1 && levels[levels.length - 2].infinite()) {
            levels[levels.length - 1] = levels[levels.length - 2];
            index = levels.length - 2;
        }
        levels[index] = new Experience.Level(
                index == 0 ? 100 : (levels[index - 1].exp() * 2),
                false,
                index == 0 ? "" : (levels[index - 1].chatMessage()),
                index == 0 ? "" : (levels[index - 1].sound())
        );

        this.addLevel(ui, events, index, levels);

        Data.set(this.experience, levels);

        if (levels.length == 1) {
            ui.set("#ExperienceLevelsRoot #CheckBox.Value", false);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup.Visible", false);
        }

        sendUpdate(ui, events, false);
    }

    private void handleDataEventEditInfinityExp(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        double exp;
        try {
            exp = Double.parseDouble(data.value);
            if (exp <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", "#FF3300");
            sendUpdate(ui, events, false);
            return;
        }
        ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", "#00726A");

        Experience.Level[] levels = experience.getLevels();
        if (levels.length == 0)
            levels = new Experience.Level[1];
        levels[levels.length - 1] = new Experience.Level(exp, true, "", "");

        Data.set(this.experience, levels);


        sendUpdate(ui, events, false);
    }
    private void handleDataEventSwapInfinity(SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Experience.Level[] levels = experience.getLevels();
        if (this.experience.isInfinite()) {
            if (levels.length <= 1) {
                ui.set("#ExperienceLevelsRoot #CheckBox.Value", true);
            } else {
                levels = Arrays.copyOfRange(levels, 0, levels.length - 1);
                Data.set(experience, levels);
                ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup.Visible", false);
            }
        } else {
            levels = Arrays.copyOf(levels, levels.length + 1);
            levels[levels.length - 1] = new Experience.Level(
                    0,
                    true,
                    "",
                    ""
            );
            Data.set(experience, levels);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup.Visible", true);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Value", "" + experience.getInfinityExp());
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", experience.getInfinityExp() == 0 ? "#FF3300" : "#00726A");
        }

        sendUpdate(ui, events, false);
    }

    private void openEditExperience(UICommandBuilder ui, UIEventBuilder events, Experience experience) {
        if (this.experience != null) {
            ui.remove("#ExperiencesRoot[2]");
            ui.remove("#ExperiencesRoot[1]");
            this.values.clear();
        }

        this.experience = experience;


        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_VALUES);
        ui.set("#ExperienceValuesRoot #Description.Text", experience.getInfo());
        experience.getValues().stream().sorted().forEach(key -> this.addValue(ui, events, key, experience.getValue(key)));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ExperiencesRoot #ExperienceValuesRoot #AddButton", EventData
                .of("ACTION", "addValue")
        );

        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_LEVELS);
        int i = 0;
        Experience.Level[] levels = experience.getLevels();
        for (Experience.Level level : levels) {
            if (level.infinite())
                break;
            this.addLevel(ui, events, i++, levels);
        }
        if (experience.isInfinite()) {
            ui.set("#ExperienceLevelsRoot #CheckBox.Value", true);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup.Visible", true);
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Value", "" + experience.getInfinityExp());
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", experience.getInfinityExp() == 0 ? "#FF3300" : "#00726A");
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ExperiencesRoot #ExperienceLevelsRoot #AddButton", EventData
                .of("ACTION", "addLevel")
        );
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ExperiencesRoot #ExperienceLevelsRoot #InfinityGroup #CheckBox", EventData
                .of("ACTION", "swapInfinity")
        );
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ExperiencesRoot #ExperienceLevelsRoot #InfinityGroup #Input", EventData
                .of("ACTION", "editInfinityExp")
                .put("@VALUE", "#ExperiencesRoot #ExperienceLevelsRoot #InfinityGroup #Input.Value")
        );
    }
    private void handleDataEventOpenEditExperience(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        Experience experience = Experience.getRegistry().getExperience(data.experience);
        this.openEditExperience(ui, events, experience);

        sendUpdate(ui, events, false);
    }
    private void addLevel(UICommandBuilder ui, UIEventBuilder events, int index, Experience.Level[] levels) {
        Experience.Level level = levels[index];
        double previousExp = index == 0 ? 0 : levels[index - 1].exp();

        ui.append("#ExperienceLevelsRoot #Levels", LAYOUT_EXPERIENCE_LEVEL);
        String selector = "#ExperienceLevelsRoot #Levels[" + index + "] ";

        ui.set(selector + "#FieldLabel.Text", "" + (index + 1));
        ui.set(selector + "#InputExperience.Value", String.valueOf(level.exp() - previousExp));
        ui.set(selector + "#InputSound.Value", level.sound());
        ui.set(selector + "#InputMessage.Value", level.chatMessage());

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#InputExperience", EventData
                .of("ACTION", "levelChange")
                .put("SELECTOR", String.valueOf(index))
                .put("META", "exp")
                .put("@VALUE", selector + "#InputExperience.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#InputSound", EventData
                .of("ACTION", "levelChange")
                .put("SELECTOR", String.valueOf(index))
                .put("META", "sound")
                .put("@VALUE", selector + "#InputSound.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#InputMessage", EventData
                .of("ACTION", "levelChange")
                .put("SELECTOR", String.valueOf(index))
                .put("META", "msg")
                .put("@VALUE", selector + "#InputMessage.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#InputRemove", EventData
                .of("ACTION", "levelRemove")
                .put("SELECTOR", String.valueOf(index))
        );
    }
    private void addValue(UICommandBuilder ui, UIEventBuilder events, String key, double exp) {
        ui.append("#ExperienceValuesRoot #Values", LAYOUT_EXPERIENCE_VALUE);
        int index = this.values.size();
        this.values.add(new ExpValue(key, exp));

        String selector = ExpValue.selector(index);

        ui.set(selector + "#InputKey.Value", key);
        if (exp != 0)
            ui.set(selector + "#InputValue.Value", String.valueOf(exp));

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#InputKey", EventData
                .of("ACTION", "valueChangeKey")
                .put("SELECTOR", String.valueOf(index))
                .put("@VALUE", selector + "#InputKey.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector + "#InputValue", EventData
                .of("ACTION", "valueChangeValue")
                .put("SELECTOR", String.valueOf(index))
                .put("@VALUE", selector + "#InputValue.Value")
        );
        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Remove", EventData
                .of("ACTION", "valueRemove")
                .put("SELECTOR", String.valueOf(index))
        );
    }
}
