package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
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
import me.jomi.hyspellengine.utils.EasyCodec;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/*
// TODO select list from PREFABS UI


 */

public class SpellsUIPage extends InteractiveCustomUIPage<SpellsUIPage.SpellsUIEventData> {
    public static class SpellsUIEventData {
        public static final BuilderCodec<SpellsUIEventData> CODEC = EasyCodec.create(SpellsUIEventData.class);

        @EasyCodec.ForCodec public String action;
        @EasyCodec.ForCodec public String selector;
        @EasyCodec.ForCodec public String category;
        @EasyCodec.ForCodec public String tab;
        @EasyCodec.ForCodec public String spell; // UUID
        @EasyCodec.ForCodec public String experience;
        @EasyCodec.ForCodec public String meta;
        @EasyCodec.ForCodec(dynamic = true) public String value;

    }
    public static final String LAYOUT_MAIN = "HySpellEngine/Spells/Main.ui";
    public static final String LAYOUT_SPELL = "HySpellEngine/Spells/Spell.ui";
    public static final String LAYOUT_SPELLS = "HySpellEngine/Spells/Spells.ui";
    public static final String LAYOUT_CATEGORY = "HySpellEngine/Spells/Category.ui";
    public static final String LAYOUT_SPELL_GROUP = "HySpellEngine/Spells/SpellGroup.ui";
    public static final String LAYOUT_EXPERIENCE = "HySpellEngine/Spells/Experience.ui";
    public static final String LAYOUT_EXPERIENCE_MAIN = "HySpellEngine/Spells/Experiences.ui";

    protected Category category;

    public SpellsUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpellsUIEventData.CODEC);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        ui.append(LAYOUT_MAIN);
        this.openSpells(ref, store, ui, events);
        //this.openExperiences(ref, store, ui, events);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("ACTION", "close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SpellsButton", EventData.of("ACTION", "tab").put("TAB", "spells"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ExpButton", EventData.of("ACTION", "tab").put("TAB", "exp"));
    }

    protected void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_SPELLS);

        int i = 0;
        for (Category category : Data.getCategories()) {
            ui.append("#Categories", LAYOUT_CATEGORY);
            this.addCategory(ui, events, category, i++);
        }

        if (Data.getCategories().length > 0)
            this.openCategory(ref, store, ui, events, category != null ? category : Data.getCategories()[0]);
    }
    protected void addCategory(UICommandBuilder ui, UIEventBuilder events, Category category, int index) {
        String selector = "#Categories[" + index + "] ";

        ui.set(selector + "#CategoryButton.Text", category.display().name());
        ui.set(selector + "#CategoryButton.TooltipText", category.display().description());
        ui.set(selector + "#Icon.AssetPath", category.display().icon().toString().replace('\\', '/'));

        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#CategoryButton",
                EventData.of("ACTION", "category")
                        .put("CATEGORY", String.valueOf(index)));

    }
    protected void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Category category) {
        this.category = category;

        ui.clear("#Spells");
        ui.append("#Spells", LAYOUT_SPELL_GROUP);
        this.addSpell(ref, store, ui, events, category.root(), "#Spells ");
    }
    protected void addSpell(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, SpellContext spell, String selector) {
        ui.append(selector + "#Parent", LAYOUT_SPELL);
        ui.set(selector + "#Parent #SpellButton.Text", spell.getDisplay().name());
        ui.set(selector + "#Parent #SpellButton.TooltipText", spell.getDisplay().description());
        ui.set(selector + "#Parent #Icon.AssetPath", spell.getDisplay().icon().toString().replace('\\', '/'));
        if (!spell.getSpell().canApply(spell, ref, store))
            ui.set(selector + "#Parent #SpellButton.Disabled", true);
        if (!spell.isParentLearned(ref, store)) {
            // TODO do something
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #SpellButton", EventData
                .of("ACTION", "spell")
                .put("SPELL", spell.getUuid().toString())
                .put("SELECTOR", selector)
        );

        spell.getSpell().build(spell, ref, store, ui, events, selector + "#Parent ");

        int i = 0;
        for (SpellContext child : spell.getChildren()) {
            ui.append(selector + "#Children", LAYOUT_SPELL_GROUP);
            this.addSpell(ref, store, ui, events, child, selector + "#Children[" + i++ + "] ");
        }
    }

    protected void openExperiences(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_EXPERIENCE_MAIN);

        AtomicInteger i = new AtomicInteger();
        Experience.getRegistry().forEach((id, experience) -> {
            if (!experience.isVisible())
                return;
            ui.append("#Experiences", LAYOUT_EXPERIENCE);
            String selector = "#Experiences[" + i.getAndIncrement() + "] ";
            this.openExperience(ref, store, ui, events, experience, selector);
        });
    }
    protected void openExperience(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Experience experience, String selector) {
        int lvl = experience.getLevel(ref, store);
        double exp = experience.getExp(ref, store);
        double nextExp = experience.getExpForNextLevel(ref, store);
        double percent = 1;


        String visibleExp;
        if (nextExp == -1) {
            visibleExp = "MAX";
        } else {
            double previousExp = lvl == 0 ? 0 : experience.getExpNeededForLevel(lvl - 1);
            visibleExp = (nextExp - previousExp) + " / " + (exp - previousExp);
            if (exp - previousExp == 0)
                percent = 0;
            else
                percent = (nextExp - previousExp) / (exp - previousExp);
            visibleExp += " (" + ((int) (percent * 100)) + "%)";
        }

        percent = Math.min(1, Math.max(0, percent));

        ui.set(selector + "#ExperienceNameLabel.Text", experience.getName());
        ui.set(selector + "#ExperienceLevelLabel.Text", lvl + " lv");
        ui.set(selector + "#ExperienceExpLabel.Text", visibleExp);
        ui.set(selector + "#ProgressBar.Value", (float) percent);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        switch (data.action) {
            case "category" -> this.handleDataEventCategory(ref, store, data);
            case "tab"      -> this.handleDataEventTab(ref, store, data);
            case "spell"    -> this.handleDataEventSpell(ref, store, data);
            case "close"    -> this.close();
            default         -> throw new IllegalStateException("Unexpected action value: " + data.action);
        }
    }
    protected void handleDataEventCategory(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        Category category = Data.getCategories()[Integer.valueOf(data.category)];
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.openCategory(ref, store, ui, events, category);
        this.sendUpdate(ui, events, false);
    }
    protected void handleDataEventTab(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        if ("spells".equals(data.tab)) {
            this.openSpells(ref, store, ui, events);
        } else if ("exp".equals(data.tab)) {
            this.openExperiences(ref, store, ui, events);
        } else {
            throw new IllegalStateException("Unexpected tab value: " + data.tab);
        }

        if (ui.getCommands().length > 0)
            this.sendUpdate(ui, events, false);
    }
    protected void handleDataEventSpell(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        SpellContext spell = category.getSpell(UUID.fromString(data.spell));

        Experience.ExperienceComponent exp = spell.getCategory().experience().getComponent(ref, store);

        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        //if (exp.getUnspendPoints(spell.getCategory().experience()) <= 0) // TODO uncomment
            if (spell.isParentLearned(ref, store) && spell.getSpell().canApply(spell, ref, store)) {
                String action = " used ";
                if (!spell.getSpell().has(spell, ref, store)) {
                    spell.learn(ref, store);
                    action = " learned ";
                    // TODO change visibility of children
                }

                exp.addSpendPoints(spell.getCategory().experience(), 1);

                spell.getSpell().apply(spell, ref, store);
                HySpellEnginePlugin.log(playerRef.getUsername() + action + spell.getDisplay().name() + " spell in " + spell.getCategory().display().name());

                ui.clear(data.selector.trim());
                ui.append(data.selector.trim(), LAYOUT_SPELL_GROUP);
                this.addSpell(ref, store, ui, events, spell, data.selector);
            }

        HySpellEnginePlugin.debugLog("Spell " + spell.getDisplay().name() + " clicked at " + category.display().name());

        sendUpdate(ui, events, false);
    }
}
