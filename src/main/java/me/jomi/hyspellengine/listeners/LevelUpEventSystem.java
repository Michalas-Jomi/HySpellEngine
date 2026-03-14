package me.jomi.hyspellengine.listeners;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.events.LevelUpEvent;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class LevelUpEventSystem extends EntityEventSystem<EntityStore, LevelUpEvent> {
    public LevelUpEventSystem() {
        super(LevelUpEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl LevelUpEvent event) {
        if (!event.getExperience().isVisible())
            return;
        if (event.getNewLevel() <= 0)
            return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        PlayerRef player = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

        Experience.Level[] levels = event.getExperience().getLevels();
        Experience.Level level = levels[Math.min(levels.length - 1, event.getNewLevel() - 1)];

        if (!level.chatMessage().isBlank())
            player.sendMessage(Message.raw(level.chatMessage()));

        if (!level.sound().isBlank()) {
            int sound = SoundEvent.getAssetMap().getIndex(level.sound());
            SoundUtil.playSoundEvent2dToPlayer(player, sound, SoundCategory.SFX);
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
