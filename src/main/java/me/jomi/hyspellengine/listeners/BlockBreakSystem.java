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

import java.util.Set;

public class BlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public BlockBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent event) {
        if ("Empty".equals(event.getBlockType().getId()))
            return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);


        String group = event.getBlockType().getGroup() != null ? "#" + event.getBlockType().getGroup() : null;
        String id = event.getBlockType().getId();

        double mining;
        double farming;
        if (group != null) {
            mining = HySpellEnginePlugin.Experiences.mining.findBest(Set.of(group, id, "*")::contains);
            farming = HySpellEnginePlugin.Experiences.farming.findBest(Set.of(group, id)::contains);
        } else {
            mining = HySpellEnginePlugin.Experiences.mining.findBest(Set.of(id, "*")::contains);
            farming = HySpellEnginePlugin.Experiences.farming.getValue(id);
        }

        if (event.getBlockType().getFarming() != null) {
            // how to use it better with cur block grow state
            farming = Math.max(farming, HySpellEnginePlugin.Experiences.farming.getValue("*"));
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
