package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.events.ExperienceChangeEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Set;

public class ExperienceChangeEventSystem extends EntityEventSystem<EntityStore, ExperienceChangeEvent> {
    private static final Experience ANY = HySpellEnginePlugin.Experiences.any;

    public ExperienceChangeEventSystem() {
        super(ExperienceChangeEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl ExperienceChangeEvent event) {
        if (event.getMethod() != ExperienceChangeEvent.Method.ADD)
            return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i); // TODO move ref below ifs
        PlayerRef player = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        HySpellEnginePlugin.debugLog("[Event] " + player.getUsername() + " gained " + event.getExp() + " xp for " + event.getExperience().getName());

        if (event.getExperience() == ANY)
            return;
        if (event.isCancelled())
            return;

        double exp = event.getExp();

        if (!ANY.getValues().isEmpty()) {
            String name = event.getExperience().getName();
            exp *= ANY.containsValue(name) ? ANY.getValue(name) : 0;
        }

        ANY.addExp(ref, commandBuffer, exp);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @NonNullDecl
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.lastSet();
    }
}
