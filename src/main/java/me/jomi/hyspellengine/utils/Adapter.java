package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.concurrent.TimeUnit;

public class Adapter {
    public static Player player(PlayerRef player) {
        return Adapter.player(player.getReference());
    }
    public static Player player(Ref<EntityStore> ref) {
        return ref.getStore().getComponent(ref, Player.getComponentType());
    }

    public static PlayerRef playerRef(Player player) {
        return playerRef(player.getReference());
    }
    public static PlayerRef playerRef(Ref<EntityStore> ref) {
        return ref.getStore().getComponent(ref, PlayerRef.getComponentType());
    }

    public static EntityStatMap stats(Player player) {
        return stats(player.getReference());
    }
    public static EntityStatMap stats(PlayerRef player) {
        return stats(player.getReference());
    }
    public static EntityStatMap stats(Ref<EntityStore> ref) {
        return ref.getStore().getComponent(ref, EntityStatMap.getComponentType());
    }

    public static World world(PlayerRef playerRef) {
        return Universe.get().getWorld(playerRef.getWorldUuid());
    }

    public static void teleport(PlayerRef player, double x, double y, double z) {
        teleport(player, Universe.get().getWorld(player.getWorldUuid()), x, y, z);
    }
    public static void teleport(PlayerRef player, World world, double x, double y, double z) {
        World from = Universe.get().getWorld(player.getWorldUuid());
        Adapter.executeSave(from, () -> {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = from.getEntityStore().getStore();

            TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
            HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
            Vector3d previousPos = transformComponent.getPosition().clone();
            Vector3f previousHeadRotation = headRotationComponent.getRotation().clone();

            //store.removeComponentIfExists(ref, Teleport.getComponentType());

            //store.removeComponentIfExists(ref, TeleportHistory.getComponentType());

            store.ensureAndGetComponent(ref, TeleportHistory.getComponentType())
                    .append(world, previousPos, previousHeadRotation, String.format("Teleport to %s (%s, %s, %s)", world.getName(), x, y, z));

            store.addComponent(
                    ref,
                    Teleport.getComponentType(),
                    Teleport.createForPlayer(world, new Vector3d(x, y, z), new Vector3f())
            );

            // Universe.get().addWorld(name, generatorType, chunkStorageType)

        });
    }


    public static void schedule(long seconds, Runnable runnable) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }, seconds, TimeUnit.SECONDS);
    }
    public static void execute(World world, Runnable runnable) {
        RuntimeException e0 = new RuntimeException("joined exception");
        world.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                e0.printStackTrace();
                e.printStackTrace();
                throw e;
            }
        });
    }
    public static void executeSave(World world, Runnable runnable) {
        if (world.isInThread())
            runnable.run();
        else
            Adapter.execute(world, runnable);
    }


    public static <T> T cast(Object obj) {
        return (T) obj;
    }
}
