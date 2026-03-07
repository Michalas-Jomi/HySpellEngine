package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.ComponentSpell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class ParticleSpell extends ComponentSpell<ParticleSpell.ParticleSpellComponent> {
    public static class ParticleSpellComponent implements Component<EntityStore> {
        public static final BuilderCodec<ParticleSpellComponent> CODEC = EasyCodec.create(ParticleSpellComponent.class);
        public static ComponentType<EntityStore, ParticleSpellComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(ParticleSpellComponent.class);
        }

        @EasyCodec.ForCodec public String name;
        @EasyCodec.ForCodec public float scale;
        @EasyCodec.ForCodec public double x;
        @EasyCodec.ForCodec public double y;
        @EasyCodec.ForCodec public double z;

        @NullableDecl
        @Override
        public Component<EntityStore> clone() {
            ParticleSpellComponent copy = new ParticleSpellComponent();
            copy.name = this.name;
            copy.scale = this.scale;
            copy.x = this.x;
            copy.y = this.y;
            copy.z = this.z;
            return copy;
        }
    }

    private final SpellField<String> nameField;
    private final SpellField<Double> xField;
    private final SpellField<Double> yField;
    private final SpellField<Double> zField;
    private final SpellField<Double> scaleField;

    public ParticleSpell() {
        super("particle", "spawns particle at player position", ParticleSpellComponent.getComponentType());
        this.nameField = this.requireFieldString("particle");
        this.xField = this.requireFieldDouble("x offset");
        this.yField = this.requireFieldDouble("y offset");
        this.zField = this.requireFieldDouble("z offset");
        this.scaleField = this.requireFieldDouble("scale");
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store, ParticleSpellComponent component) {
        component.name = this.nameField.getValue(context);
        component.x = this.xField.getValue(context);
        component.y = this.yField.getValue(context);
        component.z = this.zField.getValue(context);
        component.scale = (float) (double) this.scaleField.getValue(context);
    }

    @Override
    public boolean canApply(SpellContext spellContext, Ref<EntityStore> ref, Store<EntityStore> store) {
        return store.getComponent(ref, ParticleSpellComponent.getComponentType()) == null;
    }
}
