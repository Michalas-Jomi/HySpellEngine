package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.LeveledSpell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

public class StatsSpell extends LeveledSpell {
    private final SpellField<double[]> levelsField;
    private final SpellField<String> statField; // TODO enum

    public StatsSpell() {
        super("Stat", "Increase player stat");
        this.statField = this.requireFieldString("Stat");
        this.levelsField = this.requireField("levels", Codec.DOUBLE_ARRAY);
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level) {
        double[] boosts = this.levelsField.getValue(context);
        String stat = statField.getValue(context);
        double boost = boosts[level - 1];

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        stats.putModifier(
                statToId(stat),
                context.uuid().toString(),
                new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, (float) boost));
    }
    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        String stat = statField.getValue(context);

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        stats.removeModifier(
                statToId(stat),
                context.uuid().toString()
        );
    }

    private static int statToId(String stat) {
        switch (stat.toLowerCase()) {
            case "health":
            case "hp":
                return DefaultEntityStatTypes.getHealth();
            case "mana":
            case "mp":
                return DefaultEntityStatTypes.getMana();
            case "oxygen":
                return DefaultEntityStatTypes.getOxygen();
            case "stamina":
            case "energy":
                return DefaultEntityStatTypes.getStamina();
            case "signatureenergy":
                return DefaultEntityStatTypes.getSignatureEnergy();
            case "ammo":
            case "ammunition":
                return DefaultEntityStatTypes.getAmmo();
        }
        throw new IllegalArgumentException("unknown player stat: " + stat);
    }
}
