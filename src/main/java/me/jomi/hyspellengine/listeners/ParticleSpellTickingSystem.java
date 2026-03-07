package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.spells.ParticleSpell;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class ParticleSpellTickingSystem extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player p = store.getComponent(ref, Player.getComponentType());
        ParticleSpell.ParticleSpellComponent particleComponent = store.getComponent(ref, ParticleSpell.ParticleSpellComponent.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3f rotation = transform.getRotation();

        ParticleUtil.spawnParticleEffect(
                particleComponent.name,
                transform.getPosition().clone().add(new Vector3d(
                        particleComponent.x,
                        particleComponent.y,
                        particleComponent.z
                )),
                rotation.getYaw(),
                rotation.getPitch(),
                rotation.getRoll(),
                particleComponent.scale,
                null,
                p.getWorld().getPlayerRefs().stream().map(PlayerRef::getReference).toList(),
                store
        );


    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return ParticleSpell.ParticleSpellComponent.getComponentType();
    }
}
