package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.builtin.adventure.memories.page.MemoriesPage;
import com.hypixel.hytale.builtin.adventure.shop.ShopAsset;
import com.hypixel.hytale.builtin.adventure.shop.ShopPage;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.ui.PrefabEditorSaveSettingsPage;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.ui.PrefabTeleportPage;
import com.hypixel.hytale.builtin.buildertools.prefablist.PrefabSavePage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.RespawnPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
// TODO select list from PREFABS UI


 */

public class SpellsUIPage extends InteractiveCustomUIPage<SpellsUIPage.SpellsUIEventData> {
    public static class SpellsUIEventData {
        public static final BuilderCodec<SpellsUIEventData> CODEC = EasyCodec.create(SpellsUIEventData.class);

        @EasyCodec.ForCodec public String action;
        @EasyCodec.ForCodec public String category;
        @EasyCodec.ForCodec public String tab;
        @EasyCodec.ForCodec public String spell; // UUID

    }
    public static final String LAYOUT_MAIN = "HySpellEngine/Spells/Main.ui";
    public static final String LAYOUT_SPELL = "HySpellEngine/Spells/Spell.ui";
    public static final String LAYOUT_SPELLS = "HySpellEngine/Spells/Spells.ui";
    public static final String LAYOUT_CATEGORY = "HySpellEngine/Spells/Category.ui";
    public static final String LAYOUT_SPELL_GROUP = "HySpellEngine/Spells/SpellGroup.ui";
    public static final String LAYOUT_EXPERIENCE = "HySpellEngine/Spells/Experience.ui";
    public static final String LAYOUT_EXPERIENCE_MAIN = "HySpellEngine/Spells/Experiences.ui";

    private Category category;

    private final Category[] categories;
    public SpellsUIPage(@NonNullDecl PlayerRef playerRef, Category[] categories) { // TODO param categories is temp
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpellsUIEventData.CODEC);
        this.categories = categories;
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

    private void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_SPELLS);

        int i = -1;
        for (Category category : categories) {
            ui.append("#Categories", LAYOUT_CATEGORY);
            String selector = "#Categories[" + ++i + "] ";
            ui.set(selector + "#CategoryButton.Text", category.display().name());
            ui.set(selector + "#CategoryButton.TooltipText", category.display().description());
            ui.set(selector + "#Icon.AssetPath", category.display().icon().toString().replace('\\', '/'));

            events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#CategoryButton",
                    EventData.of("ACTION", "category")
                            .put("CATEGORY", String.valueOf(i)));
        }

        if (categories.length > 0)
            this.openCategory(ref, store, ui, events, categories[0]);
    }
    private void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, Category category) {
        this.category = category;

        ui.clear("#Spells");
        ui.append("#Spells", LAYOUT_SPELL_GROUP);
        this.addSpell(ref, store, ui, events, category.root(), "#Spells ");
    }
    private void addSpell(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events, SpellContext spell, String selector) {
        ui.append(selector + "#Parent", LAYOUT_SPELL);
        ui.set(selector + "#Parent #SpellButton.Text", spell.getDisplay().name());
        ui.set(selector + "#Parent #SpellButton.TooltipText", spell.getDisplay().description());
        ui.set(selector + "#Parent #Icon.AssetPath", spell.getDisplay().icon().toString().replace('\\', '/'));


        events.addEventBinding(CustomUIEventBindingType.Activating, selector + "#Parent #SpellButton", EventData.of("ACTION", "spell").put("SPELL", spell.getUuid().toString()));

        // commandBuilder.set(selector + "#CategoryIcon.AssetPath",
        // "UI/Custom/Pages/Memories/categories/" + category + "Complete.png"
        // );

        int i = 0;
        for (SpellContext child : spell.getChildren()) {
            ui.append(selector + "#Children", LAYOUT_SPELL_GROUP);
            this.addSpell(ref, store, ui, events, child, selector + "#Children[" + i++ + "] ");
        }
    }

    private void openExperiences(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, UIEventBuilder events) {
        this.category = null;

        ui.clear("#Container");
        ui.append("#Container", LAYOUT_EXPERIENCE_MAIN);

        List<Experience> experiences = new ArrayList<>();
        for (Category category : categories) {
            if (!experiences.contains(category.experience()))
                experiences.add(category.experience());
        }

        int i = -1;
        for (Experience experience : experiences) {
            int lvl = experience.getLevel(ref, store);
            double exp = experience.getExp(ref, store);
            double nextExp = experience.getExpForNextLevel(ref, store);
            double percent = .45f; // TODO change to 1


            String visibleExp;
            if (nextExp == -1) {
                visibleExp = "MAX";
            } else {
                double previousExp = lvl == 0 ? 0 : experience.getLevels()[lvl - 1].exp();
                visibleExp = (nextExp - previousExp) + " / " + (exp - previousExp);
                if (exp - previousExp == 0)
                    percent = 0;
                else
                    percent = (nextExp - previousExp) / (exp - previousExp);
                percent *= 100;
                visibleExp += " (" + ((int) percent) + "%)";
            }

            ui.append("#Experiences", LAYOUT_EXPERIENCE);
            String selector = "#Experiences[" + ++i + "] ";
            ui.set(selector + "#ExperienceNameLabel.Text", experience.name());
            ui.set(selector + "#ExperienceLevelLabel.Text", lvl + " lv");
            ui.set(selector + "#ExperienceExpLabel.Text", visibleExp);
            ui.set(selector + "#ProgressBar.Value", (float) percent);
        }
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
    private void handleDataEventCategory(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        Category category = this.categories[Integer.valueOf(data.category)];
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.openCategory(ref, store, ui, events, category);
        this.sendUpdate(ui, events, false);
    }
    private void handleDataEventTab(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        if ("spells".equals(data.tab)) {
            this.openSpells(ref, store, ui, events);
        } else if ("exp".equals(data.tab)) {
            this.openExperiences(ref, store, ui, events);
        } else {
            throw new IllegalStateException("Unexpected tab value: " + data.tab);
        }

        this.sendUpdate(ui, events, false);
    }
    private void handleDataEventSpell(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        SpellContext spell = category.getSpell(UUID.fromString(data.spell));

        HySpellEnginePlugin.debugLog("Spell " + spell.getDisplay().name() + " clicked at " + category.display().name());

        sendUpdate();
    }
}
