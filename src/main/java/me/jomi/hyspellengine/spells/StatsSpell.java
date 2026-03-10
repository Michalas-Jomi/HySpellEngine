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

import java.util.Arrays;

public class StatsSpell extends LeveledSpell {
    public static enum Stats {
        Health(DefaultEntityStatTypes.getHealth()),
        Stamina(DefaultEntityStatTypes.getStamina()),
        Mana(DefaultEntityStatTypes.getMana()),
        Ammo(DefaultEntityStatTypes.getAmmo()),
        Oxygen(DefaultEntityStatTypes.getOxygen()),
        SignatureEnergy(DefaultEntityStatTypes.getSignatureEnergy());

        public final int index;
        Stats(int index) {
            this.index = index;
        }
    }

    private final SpellField<double[]> levelsField;
    private final SpellField<Stats> statField;
    private final SpellField<StaticModifier.CalculationType> methodField;

    public StatsSpell() {
        super("Stat", "Increase player stat");
        this.statField = this.requireFieldEnum("Stat", Stats.class);
        this.levelsField = this.requireField("levels", Codec.DOUBLE_ARRAY,
                array -> String.join(", ", Arrays.stream(array).mapToObj(String::valueOf).toList()),
                str -> Arrays.stream(str.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray()
        );
        this.methodField = this.requireFieldEnum("method", StaticModifier.CalculationType.class);
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, int level) {
        StaticModifier.CalculationType method = methodField.getValue(context);
        double[] boosts = this.levelsField.getValue(context);
        Stats stat = statField.getValue(context);
        double boost = boosts[level - 1];

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        stats.putModifier(
                stat.index,
                context.getUuid().toString(),
                new StaticModifier(Modifier.ModifierTarget.MAX, method, (float) boost));
    }
    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Stats stat = statField.getValue(context);

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        stats.removeModifier(
                stat.index,
                context.getUuid().toString()
        );
    }

    @Override
    public boolean validate(SpellContext context) throws Throwable {
        return this.levelsField.getValue(context).length == this.maxLevelField.getValue(context);
    }
}
