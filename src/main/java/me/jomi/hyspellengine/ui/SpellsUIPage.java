package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.utils.Adapter;
import me.jomi.hyspellengine.utils.EasyCodec;
import me.jomi.hyspellengine.utils.UIBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

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

    private static final Map<String, BiConsumer<? extends SpellsUIPage, SpellsUIEventData>> dataEvents = new HashMap<>();
    protected static <T extends SpellsUIPage> void bindEvent(Class<T> clazz, String action, BiConsumer<T, SpellsUIEventData> work) {
        if (dataEvents.containsKey(action))
            throw new IllegalArgumentException("every keys in data handling must be unique, key: " + action);
        dataEvents.put(action, work);
    }


    protected Category category;
    protected UIBuilder ui;


    public SpellsUIPage(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpellsUIEventData.CODEC);
    }

    @Override
    protected void sendUpdate() {
        if (this.ui == null)
            super.sendUpdate();
        else
            this.sendUpdate(this.ui.ui(), this.ui.events(), false);
    }

    /// clear -> append -> clear -> work = Failed to apply Custom UI Commands
    protected final void doWithOther(Runnable work) {
        UIBuilder previous = this.ui;
        this.ui = new UIBuilder();

        work.run();
        this.sendUpdate();

        this.ui = previous;
    }


    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiBuilder, @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        this.ui = new UIBuilder(uiBuilder, events);

        ui.append(LAYOUT_MAIN);
        this.openSpells();
        //this.openExperiences();

        ui.onClick("#SpellsButton", "tab", "TAB", "spells");
        ui.onClick("#ExpButton", "tab", "TAB", "exp");
    }

    protected void openSpells() {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_SPELLS);

        int i = 0;
        for (Category category : Data.getCategories()) {
            ui.append("#Categories", LAYOUT_CATEGORY);
            this.addCategory(ui.at("#Categories", i), category, i++);
        }

        if (Data.getCategories().length > 0)
            this.openCategory(category != null ? category : Data.getCategories()[0]);
    }
    protected void addCategory(UIBuilder ui, Category category, int index) {
        ui.set("#CategoryButton.Text", category.display().name());
        ui.set("#CategoryButton.TooltipText", category.display().description());
        ui.set("#Icon.AssetPath", category.display().icon().toString().replace('\\', '/'));

        this.updatePoints(category);

        ui.onClick("#CategoryButton", "category", "CATEGORY", String.valueOf(index));
    }
    protected void openCategory(Category category) {
        this.category = category;

        ui.clear("#Spells");
        ui.append("#Spells", LAYOUT_SPELL_GROUP);
        this.addSpell(category.root(), ui.at("#Spells"));
    }
    protected void addSpell(SpellContext spell, UIBuilder ui) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        UIBuilder uiParent = ui.at("#Parent");
        uiParent.append(LAYOUT_SPELL);
        uiParent.set("#TitleLabel.Text", spell.getDisplay().name());
        uiParent.set("#SpellButton.TooltipText", spell.getDisplay().description());
        uiParent.set("#Icon.AssetPath", spell.getDisplay().icon().toString().replace('\\', '/'));
        uiParent.set("#Lock.Visible", !spell.isParentLearned(ref, store));
        if (!spell.getSpell().canApply(spell, ref, store))
            uiParent.set("#SpellButton.Disabled", true);

        uiParent.onClick("#SpellButton", "spell", "SPELL", spell.getUuid().toString(), "SELECTOR", ui.selector());

        spell.getSpell().build(spell, ref, store, uiParent);

        int i = 0;
        for (SpellContext child : spell.getChildren()) {
            ui.append("#Children", LAYOUT_SPELL_GROUP);
            this.addSpell(child, ui.at("#Children[" + i++ + "]"));
        }
    }

    protected void openExperiences() {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_EXPERIENCE_MAIN);

        AtomicInteger i = new AtomicInteger();
        Experience.getRegistry().forEach((id, experience) -> {
            if (!experience.isVisible())
                return;
            ui.append("#Experiences", LAYOUT_EXPERIENCE);
            this.openExperience(ui.at("#Experiences[" + i.getAndIncrement() + "]"), experience);
        });
    }
    protected void openExperience(UIBuilder ui, Experience experience) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        int lvl = experience.getLevel(ref, store);
        double fullExp = experience.getExp(ref, store);
        double nextFullExp = experience.getExpForNextLevel(ref, store);
        double percent = 1;
        String visibleExp;

        if (!experience.canReachNextLevel(ref, store)) {
            visibleExp = "MAX";
        } else {
            double previousFullExp = experience.getExpForLevel(lvl);
            double exp = fullExp - previousFullExp;
            double nextExp = nextFullExp - previousFullExp;

            visibleExp = Math.round(exp) + " / " + Math.round(nextExp);
            if (nextExp == 0) // don't divide by 0
                percent = 0;
            else
                percent = exp / nextExp;

            visibleExp += " (" + Math.round(percent * 100) + "%)";
        }

        percent = Math.min(1, Math.max(0, percent));

        ui.set("#ExperienceNameLabel.Text", experience.getName());
        ui.set("#ExperienceLevelLabel.Text", lvl + " lv");
        ui.set("#ExperienceExpLabel.Text", visibleExp);
        ui.set("#ProgressBar.Value", percent);
    }

    @Override
    public final void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {
        this.ui = new UIBuilder();

        dataEvents.get(data.action).accept(Adapter.cast(this), data);

        this.sendUpdate(this.ui.ui(), this.ui.events(), false);

        this.ui = null;
    }

    protected void handleDataEventCategory(SpellsUIEventData data) {
        Category category = Data.getCategories()[Integer.valueOf(data.category)];
        this.openCategory(category);
    }
    protected void handleDataEventTab(SpellsUIEventData data) {
        if ("spells".equals(data.tab)) {
            this.openSpells();
        } else if ("exp".equals(data.tab)) {
            this.openExperiences();
        } else {
            throw new IllegalStateException("Unexpected tab value: " + data.tab);
        }
    }
    protected void handleDataEventSpell(SpellsUIEventData data) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        SpellContext spell = category.getSpell(UUID.fromString(data.spell));

        if (spell.getCategory().experience().getUnspendPoints(ref, store, spell.getCategory()) <= 0)
            return;
        if (!spell.isParentLearned(ref, store))
            return;
        if (!spell.getSpell().canApply(spell, ref, store))
            return;

        String action = " used ";
        if (!spell.getSpell().has(spell, ref, store)) {
            spell.learn(ref, store);
            action = " learned ";
        }

        spell.getCategory().experience().addSpendPoints(ref, store, spell.getCategory(), 1);
        this.updatePoints(spell.getCategory());

        spell.getSpell().apply(spell, ref, store);
        HySpellEnginePlugin.log(this.playerRef.getUsername() + action + spell.getDisplay().name() + " spell in " + spell.getCategory().display().name());

        UIBuilder ui = this.ui.at(data.selector);
        ui.clear();
        ui.append(LAYOUT_SPELL_GROUP);
        this.addSpell(spell, ui);
    }

    protected void updatePoints(Category category) {
        int i = -1;
        for (Category cat : Data.getCategories()) {
            i++;
            if (cat == category)
                break;
        }
        if (i == -1)
            return;

        int points = category.experience().getUnspendPoints(playerRef.getReference(), playerRef.getReference().getStore(), category);

        UIBuilder ui = this.ui.at("#Categories", i).at("#Points");
        ui.set(".Visible", points > 0);
        if (points > 0)
            ui.set("#PointsLabel.Text", "" + points);
    }

    static {
        bindEvent(SpellsUIPage.class, "category", SpellsUIPage::handleDataEventCategory);
        bindEvent(SpellsUIPage.class, "tab", SpellsUIPage::handleDataEventTab);
        bindEvent(SpellsUIPage.class, "spell", SpellsUIPage::handleDataEventSpell);
    }
}
