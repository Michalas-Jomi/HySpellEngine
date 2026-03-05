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
import com.hypixel.hytale.server.npc.pages.EntitySpawnPage;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.atomic.AtomicInteger;

/*
// TODO select list from PREFABS UI


 */

public class SpellsUIPage extends InteractiveCustomUIPage<SpellsUIPage.SpellsUIEventData> {
    public static class SpellsUIEventData {
        public static final BuilderCodec<SpellsUIEventData> CODEC = EasyCodec.create(SpellsUIEventData.class);
    }
    public static final String LAYOUT_MAIN = "HySpellEngine/Spells/Main.ui";
    public static final String LAYOUT_SPELL = "HySpellEngine/Spells/Spell.ui";
    public static final String LAYOUT_SPELLS = "HySpellEngine/Spells/Spells.ui";
    public static final String LAYOUT_CATEGORY = "HySpellEngine/Spells/Category.ui";
    public static final String LAYOUT_SPELL_GROUP = "HySpellEngine/Spells/SpellGroup.ui";

    private final Category[] categories;
    public SpellsUIPage(@NonNullDecl PlayerRef playerRef, Category[] categories) { // TODO param categories is temp
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpellsUIEventData.CODEC);
        this.categories = categories;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder ui, @NonNullDecl UIEventBuilder event, @NonNullDecl Store<EntityStore> store) {
        ui.append(LAYOUT_MAIN);
        this.openSpells(ref, store, ui);
    }

    private void openSpells(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui) {
        ui.clear("#Container");
        ui.append("#Container", LAYOUT_SPELLS);

        int i=0;
        for (Category category : categories) {
            ui.append("#Categories", LAYOUT_CATEGORY);
            String selector = "#Categories[" + i++ + "] ";
            ui.set(selector + "#CategoryButton.Text", category.display().name());
        }

        if (categories.length > 0)
            this.openCategory(ref, store, ui, categories[0]);
    }
    private void openCategory(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, Category category) {
        ui.clear("#Spells");
        ui.append("#Spells", LAYOUT_SPELL_GROUP);
        this.addSpell(ref, store, ui, category.root(), "#Spells ");
    }
    private void addSpell(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder ui, SpellContext spell, String selector) {
        ui.append(selector + "#Parent", LAYOUT_SPELL);
        ui.set(selector + "#Parent #SpellButton.Text", spell.getDisplay().name());
        //ui.set(selector + ".Anchor.Left", spell.getDisplay().x());
        //ui.set(selector + ".Anchor.Top", spell.getDisplay().y());

        int i = 0;
        for (SpellContext child : spell.getChildren()) {
            ui.append(selector + "#Children", LAYOUT_SPELL_GROUP);
            this.addSpell(ref, store, ui, child, selector + "#Children[" + i++ + "] ");
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, SpellsUIEventData data) {

    }
}
