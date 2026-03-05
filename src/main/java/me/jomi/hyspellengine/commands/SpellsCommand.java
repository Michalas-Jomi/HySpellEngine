package me.jomi.hyspellengine.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.ui.SpellsUIPage;
import me.jomi.hyspellengine.utils.Adapter;
import me.jomi.hyspellengine.utils.MSG;
import me.jomi.hyspellengine.utils.PlayerCommand;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public class SpellsCommand extends PlayerCommand {
    public SpellsCommand() {
        super("spells", "Open Spells menu");
    }

    @Override
    protected void exe(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef player, @NonNullDecl World world) throws MSG {
        Player p = store.getComponent(ref, Player.getComponentType());
        p.getPageManager().openCustomPage(ref, store, new SpellsUIPage(player, new Category[]{makeExapleCategory(), makeExapleCategory(), makeExapleCategory()}));
    }

    private Category makeExapleCategory() {
        return makeExapleCategory(2);
    }
    private Category makeExapleCategory(int lvl) {
        if (lvl == 0) {
            SpellContext root = makeExampleSpellContext("deep", new SpellContext[0], 0, 0);
            return new Category(
                    new Category.Display(
                            "Cat1",
                            "Cat1 desc",
                            null
                    ),
                    new Experience("Exp1"),
                    root
            );
        }
        SpellContext childUp1   = makeExampleSpellContext("up1", new SpellContext[]{makeExapleCategory(lvl-1).root()}, 200, 200);
        SpellContext childUp2   = makeExampleSpellContext("up2", new SpellContext[]{makeExapleCategory(lvl-1).root()}, 200, 100);
        //SpellContext childUp3   = makeExampleSpellContext("up3", new SpellContext[]{makeExapleCategory(lvl-1).root()}, 200, 0);
        SpellContext childDown1 = makeExampleSpellContext("down1", new SpellContext[]{makeExapleCategory(lvl-1).root()}, 200, -100);

        SpellContext childUp   = makeExampleSpellContext("up", new SpellContext[]{childUp1, childUp2/*, childUp3*/}, 100, 100);
        SpellContext childDown = makeExampleSpellContext("down", new SpellContext[]{childDown1}, 100, -100);

        SpellContext root = makeExampleSpellContext("root", new SpellContext[]{childUp, childDown}, 0, 0);

        return new Category(
                new Category.Display(
                        "Cat1",
                        "Cat1 desc",
                        null
                ),
                new Experience("Exp1"),
                root
        );
    }
    private SpellContext makeExampleSpellContext(String name, SpellContext[] children, int x, int y) {
        BsonDocument fields = new BsonDocument();
        fields.put("permission", new BsonString("example.permission"));
        return new SpellContext(
                Spell.getSpellRegistry().getSpell("Permission"),
                new SpellContext.Display(
                        name,
                        name + "\nSpellDesc Here",
                        null,
                        x,
                        y
                ),
                UUID.randomUUID(),
                fields,
                children);
    }
}
