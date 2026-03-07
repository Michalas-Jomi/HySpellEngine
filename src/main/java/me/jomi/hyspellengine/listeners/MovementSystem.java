package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.utils.Adapter;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MovementSystem extends EntityTickingSystem<EntityStore> {
    private static record PlayerRefExp(PlayerRef player, double exp) {}
    private static final Map<UUID, Double> map = new ConcurrentHashMap<>();
    public static ScheduledFuture<?> task;

    public MovementSystem() {
        task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(MovementSystem::giveExp, 1, 300, TimeUnit.MILLISECONDS);
    }

    @Override
    public void tick(float v, int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        MovementStates move = store.getComponent(ref, MovementStatesComponent.getComponentType()).getMovementStates();
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());

        double exp;
        if (move.jumping)
            exp = 5;
        else if (move.sprinting)
            exp = 2;
        else if (move.walking)
            exp = 1;
        else
            return;

        map.merge(player.getUuid(), exp * v, Double::sum);
    }

    private static void giveExp() {
        Map<UUID, List<PlayerRefExp>> worlds = new HashMap<>();

        map.forEach((playerUUID, exp) -> {
            PlayerRef player = Universe.get().getPlayer(playerUUID);
            if (player == null)
                return;

            worlds.computeIfAbsent(player.getWorldUuid(), k -> new ArrayList<>())
                    .add(new PlayerRefExp(player, exp.doubleValue()));
        });
        map.clear();

        worlds.forEach((worldUUID, entries) -> {
            World world = Universe.get().getWorld(worldUUID);

            Adapter.execute(world, () -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                for (PlayerRefExp entry : entries) {
                    HySpellEnginePlugin.Experiences.moving.addExp(entry.player().getReference(), store, entry.exp());
                }
            });
        });
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
