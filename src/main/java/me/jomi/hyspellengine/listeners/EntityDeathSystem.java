package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class EntityDeathSystem extends DeathSystems.OnDeathSystem {
    @Override
    public void onComponentAdded(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl DeathComponent deathComponent, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        if (deathComponent.getDeathInfo().getSource() instanceof Player player) {
            boolean playerKill = commandBuffer.getComponent(ref, Player.getComponentType()) != null;
            HySpellEnginePlugin.Experiences.combat.addExp(player.getReference(), commandBuffer, playerKill ? 100 : 20);
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player != null)
            HySpellEnginePlugin.Experiences.dying.addExp(ref, commandBuffer, 1);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(NPCEntity.getComponentType(), Player.getComponentType());
    }
}
