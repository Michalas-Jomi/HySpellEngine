package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class BlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public BlockBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent breakBlockEvent) {
        if ("Empty".equals(breakBlockEvent.getBlockType().getId()))
            return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);

        double mining = 0;
        double farming = 0;

        if (breakBlockEvent.getBlockType().getGroup() != null)
            switch (breakBlockEvent.getBlockType().getGroup()) {
                case "Gravel":
                case "Grass":
                    mining = 1;
                    break;
                case "Stone":
                    mining = 2;
                    break;
                case "Wood":
                    farming = 1;
                    break;
            }

        if (breakBlockEvent.getBlockType().getFarming() != null) {
            farming += 20;
        }

        if (mining != 0)
            HySpellEnginePlugin.Experiences.mining.addExp(ref, commandBuffer, mining);
        if (farming != 0)
            HySpellEnginePlugin.Experiences.farming.addExp(ref, commandBuffer, farming);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
