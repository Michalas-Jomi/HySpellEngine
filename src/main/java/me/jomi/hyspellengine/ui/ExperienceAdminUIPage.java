package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.utils.Adapter;
import me.jomi.hyspellengine.utils.UIBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExperienceAdminUIPage extends SpellsUIPage {
    protected class ExpValue {
        String node = null;
        String key;
        double exp;
        ExpValue(String key, double exp) {
            this.key = key;
            this.exp = exp;

            if (this.isKeyValid())
                this.node = this.key;
        }

        UIBuilder selector(int index) {
            return ExperienceAdminUIPage.this.ui.at("#ExperienceValuesRoot #Values", index);
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

            if (!ExperienceAdminUIPage.this.experience.keyValidator.test(this.key))
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

    public static final String LAYOUT_EXPERIENCE_VALUE = "HySpellEngine/Spells/Admin/ExperienceValue.ui";
    public static final String LAYOUT_EXPERIENCE_VALUES = "HySpellEngine/Spells/Admin/ExperienceValues.ui";
    public static final String LAYOUT_EXPERIENCE_LEVEL = "HySpellEngine/Spells/Admin/ExperienceLevel.ui";
    public static final String LAYOUT_EXPERIENCE_LEVELS = "HySpellEngine/Spells/Admin/ExperienceLevels.ui";
    public static final String LAYOUT_EXPERIENCE_EDIT_BUTTON = "HySpellEngine/Spells/Admin/ExperienceEditButton.ui";

    protected Experience experience;
    protected final List<ExpValue> values = new ArrayList<>();


    public ExperienceAdminUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }


    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        super.build(ref, ui, events, store);
        this.openExperiences();
    }

    private boolean firstOpen = true;
    @Override
    protected void openSpells() {
        if (this.firstOpen) {
            this.firstOpen = false;
            return;
        }
        Ref<EntityStore> ref = this.playerRef.getReference();
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().openCustomPage(ref, store, new SpellsAdminUIPage(this.playerRef));
    }

    @Override
    protected void openExperiences() {
        ui.clear("#Container");
        ui.append("#Container", SpellsUIPage.LAYOUT_EXPERIENCE_MAIN);

        this.experience = null;

        AtomicInteger i = new AtomicInteger();
        Experience.getRegistry().forEach((id, experience) -> {
            ui.append("#Experiences", SpellsUIPage.LAYOUT_EXPERIENCE);
            UIBuilder uiExp = ui.at("#Experiences[" + i.getAndIncrement() + "]");
            this.openExperience(uiExp, experience);
            if (!experience.isVisible())
                uiExp.set("#ExperienceNameLabel.Style.TextColor", "#FF9999");
        });
    }
    @Override
    protected void openExperience(UIBuilder ui, Experience experience) {
        super.openExperience(ui, experience);

        ui.append(LAYOUT_EXPERIENCE_EDIT_BUTTON);

        ui.onClick      ("#ExperienceEditButton", "openExperienceEdit", "EXPERIENCE", experience.getName(), "SELECTOR", ui.selector());
        ui.onClickRight ("#ExperienceEditButton", "openExperienceEdit", "EXPERIENCE", experience.getName(), "SELECTOR", ui.selector());
    }


    private void handleDataEventOpenEditExperience(SpellsUIEventData data) {
        Experience experience = Experience.getRegistry().getExperience(data.experience);
        this.openEditExperience(experience);
    }

    private void handleDataEventValueRemove(SpellsUIEventData data) {
        int index = Integer.parseInt(data.selector);
        ExpValue value = this.values.get(index);

        if (value.node != null)
            Data.set(this.experience, value.node, 0);

        // value.selector(index).remove("");
        // removing requires to reindexing each event, hiding not
        value.selector(index).set(".Visible", false);
    }
    private void handleDataEventValueChangeKey(SpellsUIEventData data) {
        int index = Integer.parseInt(data.selector);
        ExpValue value = this.values.get(index);

        String previousKey = value.key;
        value.key = data.value.trim();

        for (int i = 0; i < this.values.size(); i++) {
            ExpValue v = this.values.get(i);
            if (v.key.equals(previousKey) || v.key.equals(value.key)) {
                if (v.key.equals(previousKey))
                    v.trySave();
                v.selector(i).set("#InputKey.Style.TextColor", v.isKeyValid() ? "#009900" : "#FF3300");
            }
        }

        if (value.isKeyValid())
            value.trySave();
    }
    private void handleDataEventValueChangeValue(SpellsUIEventData data) {
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

        value.selector(index).set("#InputValue.Style.TextColor", color);

        value.trySave();
    }
    private void handleDataEventAddValue(SpellsUIEventData data) {
        this.addValue("", 0);
    }

    private void handleDataEventLevelRemove(SpellsUIEventData data) {
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

        this.openExperiences();
        this.openEditExperience(experience);
    }
    private void handleDataEventLevelChange(SpellsUIEventData data) {
        int index = Integer.parseInt(data.selector);
        UIBuilder ui = this.ui.at("#ExperienceLevelsRoot #Levels", index);
        Experience.Level[] levels = this.experience.getLevels();
        Experience.Level level = levels[index];


        switch (data.meta) {
            case "exp":
                double exp;
                try {
                    exp = Double.parseDouble(data.value);
                } catch (NumberFormatException e) {
                    exp = 0;
                }

                ui.set("#InputExperience.Style.TextColor", exp <= 0 ? "#FF3300" : "#FFFFFF");
                if (exp <= 0)
                    return;

                double pre = index == 0 ? 0 : levels[index - 1].exp();
                double diff = exp - (level.exp() - pre);
                for (int i = index; i < levels.length; i++) {
                    Experience.Level lvl = levels[i];
                    levels[i] = new Experience.Level(
                            lvl.exp() + diff,
                            lvl.infinite(),
                            lvl.chatMessage(),
                            lvl.sound()
                    );
                }
                break;
            case "sound":
                boolean invalid = SoundEvent.getAssetMap().getIndexOrDefault(data.value, -1) == -1;

                ui.set("#InputSound.Style.TextColor", invalid ? "#FF3300" : "#FFFFFF");

                if (invalid) {
                    if (level.sound().isBlank())
                        return;
                    data.value = "";
                }
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
    }
    private void handleDataEventAddLevel(SpellsUIEventData data) {
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

        this.addLevel(index, levels);

        Data.set(this.experience, levels);

        if (levels.length == 1)
            this.updateInfinityVisibility();
    }

    private void handleDataEventEditInfinityExp(SpellsUIEventData data) {
        double exp;
        try {
            exp = Double.parseDouble(data.value);
            if (exp <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", "#FF3300");
            return;
        }
        ui.set("#ExperienceLevelsRoot #ExperienceInfinityGroup #Input.Style.TextColor", "#00726A");

        Experience.Level[] levels = experience.getLevels();
        if (levels.length == 0)
            levels = new Experience.Level[1];
        levels[levels.length - 1] = new Experience.Level(exp, true, "", "");

        Data.set(this.experience, levels);
    }
    private void handleDataEventSwapInfinity(SpellsUIEventData data) {
        Experience.Level[] levels = experience.getLevels();
        if (this.experience.isInfinite()) {
            if (levels.length > 1) {
                levels = Arrays.copyOfRange(levels, 0, levels.length - 1);
                Data.set(experience, levels);
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
        }
        this.updateInfinityVisibility();
    }

    private void updateInfinityVisibility() {
        UIBuilder ui = this.ui.at("#ExperienceLevelsRoot #InfinityGroup");

        ui.set("#CheckBox.Value", experience.isInfinite());
        ui.set("#ExperienceInfinityGroup.Visible", experience.isInfinite());
        if (experience.isInfinite()) {
            ui.set("#Input.Value", Adapter.formatDouble(experience.getInfinityExp()));
            ui.set("#Input.Style.TextColor", experience.getInfinityExp() == 0 ? "#FF3300" : "#00726A");
        }
    }
    private void openEditExperience(Experience experience) {
        if (this.experience != null) {
            ui.remove("#ExperiencesRoot[2]");
            ui.remove("#ExperiencesRoot[1]");
            this.values.clear();
        }

        this.experience = experience;

        UIBuilder uiValues = ui.at("#ExperiencesRoot #ExperienceValuesRoot");
        UIBuilder uiLevels = ui.at("#ExperiencesRoot #ExperienceLevelsRoot");
        UIBuilder uiInfinity = uiLevels.at("#InfinityGroup");

        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_VALUES);
        uiValues.set("#Description.Text", experience.getInfo());
        experience.getValues().stream().sorted().forEach(key -> this.addValue(key, experience.getValue(key)));
        uiValues.onClick("#AddButton", "addValue");

        ui.append("#ExperiencesRoot", LAYOUT_EXPERIENCE_LEVELS);
        int i = 0;
        Experience.Level[] levels = experience.getLevels();
        for (Experience.Level level : levels) {
            if (level.infinite())
                break;
            this.addLevel(i++, levels);
        }
        this.updateInfinityVisibility();

        uiLevels.onClick("#AddButton", "addLevel");
        uiInfinity.onCheckBox("#CheckBox", "swapInfinity");
        uiInfinity.onChange("#Input", "editInfinityExp");
    }

    private void addLevel(int index, Experience.Level[] levels) {
        Experience.Level level = levels[index];
        double previousExp = index == 0 ? 0 : levels[index - 1].exp();

        this.ui.append("#ExperienceLevelsRoot #Levels", LAYOUT_EXPERIENCE_LEVEL);
        UIBuilder ui = this.ui.at("#ExperienceLevelsRoot #Levels", index);

        ui.set("#FieldLabel.Text", "" + (index + 1));
        ui.set("#InputExperience.Value", Adapter.formatDouble(level.exp() - previousExp));
        ui.set("#InputSound.Value", level.sound());
        ui.set("#InputMessage.Value", level.chatMessage());

        ui.onChange("#InputExperience", "levelChange", "META", "exp", "SELECTOR", String.valueOf(index));

        ui.onChange("#InputSound", "levelChange", "META", "sound", "SELECTOR", String.valueOf(index));
        ui.onChange("#InputMessage", "levelChange", "META", "msg", "SELECTOR", String.valueOf(index));

        ui.onClick("#InputRemove", "levelRemove", "SELECTOR", String.valueOf(index));
    }
    private void addValue(String key, double exp) {
        ui.append("#ExperienceValuesRoot #Values", LAYOUT_EXPERIENCE_VALUE);
        int index = this.values.size();
        ExpValue value = new ExpValue(key, exp);
        this.values.add(value);

        UIBuilder ui = value.selector(index);

        ui.set("#InputKey.Value", key);
        if (exp != 0)
            ui.set("#InputValue.Value", Adapter.formatDouble(exp));

        ui.onChange("#InputKey", "valueChangeKey", "SELECTOR", String.valueOf(index));
        ui.onChange("#InputValue", "valueChangeValue", "SELECTOR", String.valueOf(index));
        ui.onClick("#Remove", "valueRemove", "SELECTOR", String.valueOf(index));
    }


    static {
        bindEvent(ExperienceAdminUIPage.class, "openExperienceEdit", ExperienceAdminUIPage::handleDataEventOpenEditExperience);

        bindEvent(ExperienceAdminUIPage.class, "addValue", ExperienceAdminUIPage::handleDataEventAddValue);
        bindEvent(ExperienceAdminUIPage.class, "valueChangeKey", ExperienceAdminUIPage::handleDataEventValueChangeKey);
        bindEvent(ExperienceAdminUIPage.class, "valueChangeValue", ExperienceAdminUIPage::handleDataEventValueChangeValue);
        bindEvent(ExperienceAdminUIPage.class, "valueRemove", ExperienceAdminUIPage::handleDataEventValueRemove);

        bindEvent(ExperienceAdminUIPage.class, "addLevel", ExperienceAdminUIPage::handleDataEventAddLevel);
        bindEvent(ExperienceAdminUIPage.class, "levelChange", ExperienceAdminUIPage::handleDataEventLevelChange);
        bindEvent(ExperienceAdminUIPage.class, "levelRemove", ExperienceAdminUIPage::handleDataEventLevelRemove);

        bindEvent(ExperienceAdminUIPage.class, "swapInfinity", ExperienceAdminUIPage::handleDataEventSwapInfinity);
        bindEvent(ExperienceAdminUIPage.class, "editInfinityExp", ExperienceAdminUIPage::handleDataEventEditInfinityExp);
    }
}
