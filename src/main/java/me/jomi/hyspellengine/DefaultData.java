package me.jomi.hyspellengine;

import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import org.bson.BsonDocument;

import java.nio.file.Path;
import java.util.UUID;

public class DefaultData {
    public static void makeExampleData() {
        Data.addCategory(categoryCombat());
    }
    private static Category categoryCombat() {
        SpellContext child1 = new SpellContext(
                Spell.getSpellRegistry().getSpell("Stat"),
                new SpellContext.Display(
                        "Growth 2",
                        "Make your bones stronger",
                        Path.of("Icons", "CraftingCategories", "Alchemy", "Combat_Potions.png")
                ),
                UUID.randomUUID(),
                BsonDocument.parse("{\"Stat\":\"Health\",\"levels\":[10.0, 20.0, 30.0],\"method\":\"Additive\",\"max level\":3}"),
                new SpellContext[0]
        );
        SpellContext child2 = new SpellContext(
                Spell.getSpellRegistry().getSpell("Stat"),
                new SpellContext.Display(
                        "Growth 3",
                        "Make your bones stronger",
                        Path.of("Icons", "CraftingCategories", "Alchemy", "Combat_Potions.png")
                ),
                UUID.randomUUID(),
                BsonDocument.parse("{\"Stat\":\"Health\",\"levels\":[10.0, 20.0, 30.0],\"method\":\"Additive\",\"max level\":3}"),
                new SpellContext[0]
        );

        SpellContext root = new SpellContext(
                Spell.getSpellRegistry().getSpell("Stat"),
                new SpellContext.Display(
                        "Growth",
                        "Make your bones stronger",
                        Path.of("Icons", "CraftingCategories", "Alchemy", "Combat_Potions.png")
                ),
                UUID.randomUUID(),
                BsonDocument.parse("{\"Stat\":\"Health\",\"levels\":[10.0, 20.0, 30.0],\"method\":\"Additive\",\"max level\":3}"),
                new SpellContext[]{child1, child2}
        );


        return new Category(
                new Category.Display(
                        "Fighting",
                        "Master your fight",
                        Path.of("Icons", "ItemCategories", "Items-Weapons.png")
                ),
                HySpellEnginePlugin.Experiences.combat,
                root,
                UUID.randomUUID()
        );
    }
}
