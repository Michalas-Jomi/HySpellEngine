package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InteractionListener {
    public static enum MouseButton {
        LEFT,
        RIGHT;
    }
    public static void setup() {
        PlayerPacketTracker.registerHandlerIn(SyncInteractionChains.class, InteractionListener::onPacket);
    }
    public static void onPacket(GamePacketHandler handler, SyncInteractionChains packet) {
        for (SyncInteractionChain chain : packet.updates) {
            if (chain == null)
                continue;
            if (chain.itemInHandId == null)
                continue;
            if (chain.interactionType != InteractionType.Primary && chain.interactionType != InteractionType.Secondary)
                continue;

            Map<String, BiConsumer<GamePacketHandler, SyncInteractionChains>> map = chain.interactionType == InteractionType.Primary ? registeredLeft : registeredRight;
            BiConsumer<GamePacketHandler, SyncInteractionChains> function = map.get(chain.itemInHandId);
            if (function != null)
                function.accept(handler, packet);
        }
    }

    private static final Map<String, BiConsumer<GamePacketHandler, SyncInteractionChains>> registeredLeft = new HashMap<>();
    private static final Map<String, BiConsumer<GamePacketHandler, SyncInteractionChains>> registeredRight = new HashMap<>();
    public static void register(@Nullable MouseButton mouseButton, String itemId, BiConsumer<GamePacketHandler, SyncInteractionChains> function) {
        if (mouseButton != MouseButton.LEFT)
            registeredRight.put(itemId, function);
        if (mouseButton != MouseButton.RIGHT)
            registeredLeft.put(itemId, function);
    }
    public static void register(String itemId, BiConsumer<GamePacketHandler, SyncInteractionChains> function) {
        InteractionListener.register(null, itemId, function);
    }
    public static void register(@Nullable MouseButton mouseButton, String itemId, Consumer<PlayerRef> function) {
        InteractionListener.register(mouseButton, itemId, (h, p) -> function.accept(h.getPlayerRef()));
    }
    public static void register(String itemId, Consumer<PlayerRef> function) {
        InteractionListener.register(null, itemId, function);
    }
}
