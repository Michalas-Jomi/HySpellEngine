package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.RepeatableSpell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

public class TeleportSpell extends RepeatableSpell {
    private final SpellField<String> worldField;
    private final SpellField<Double> xField;
    private final SpellField<Double> yField;
    private final SpellField<Double> zField;

    public TeleportSpell() {
        super("teleportTo", "repeatable, teleport player to selected location");
        this.worldField = this.requireFieldString("world");
        this.xField = this.requireFieldDouble("x offset");
        this.yField = this.requireFieldDouble("y offset");
        this.zField = this.requireFieldDouble("z offset");
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        String worldName = this.worldField.getValue(context);
        World world = Universe.get().getWorld(worldName);
        if (world == null) {
            HySpellEnginePlugin.warn("Unknown world at Teleport spell \"" + context.getDisplay().name() + "\"");
            return;
        }
        Teleport teleport = Teleport.createForPlayer(
                world,
                new Vector3d(
                        this.xField.getValue(context),
                        this.yField.getValue(context),
                        this.zField.getValue(context)
                ),
                new Vector3f()
        );
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    }
}
