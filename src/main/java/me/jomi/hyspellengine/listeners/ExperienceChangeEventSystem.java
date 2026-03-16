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
import me.jomi.hyspellengine.ui.ExperienceHUD;
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
        if (event.isCancelled())
            return;

        // HUD section
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Experience experience = event.getExperience();
        int lvl = experience.getLevel(ref, commandBuffer);

        if (lvl != experience.getMaxLevel() && experience.isVisible()) {
            double playerLvlExp = experience.getExpForLevel(lvl);
            double playerExp = experience.getExp(ref, commandBuffer) + event.getExp();

            ExperienceHUD.modify(
                    ref,
                    commandBuffer,
                    experience,
                    lvl,
                    (float) ((playerExp - playerLvlExp)
                            /
                            (experience.getExpForLevel(lvl + 1) - playerLvlExp))
            );
        }



        // Experiences.ANY section
        if (event.getExperience() == ANY)
            return;


        double exp = event.getExp();

        if (!ANY.getValues().isEmpty()) {
            String name = experience.getName();
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
