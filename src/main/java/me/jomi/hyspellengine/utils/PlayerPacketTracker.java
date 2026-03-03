package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.window.OpenWindow;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class PlayerPacketTracker {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static class PlayerStats {
        final Map<String, AtomicInteger> sent = new ConcurrentHashMap<>();
        final Map<String, AtomicInteger> received = new ConcurrentHashMap<>();
    }
    private static final Map<String, PlayerStats> stats = new ConcurrentHashMap<>();

    private static String getPlayerName(PacketHandler handler) {
        if (handler instanceof GamePacketHandler gpHandler) {
            return gpHandler.getPlayerRef().getUsername();
        }
        return null;
    }
    private static PlayerRef getPlayerRef(PacketHandler handler) {
        if (handler instanceof GamePacketHandler gpHandler) {
            return gpHandler.getPlayerRef();
        }
        return null;
    }

    private static final Set<String> ignored = new HashSet<>();
    public static void registerPacketCounters() {
        PacketAdapters.registerInbound((PacketHandler handler, Packet packet) -> {
            var packetName = packet.getClass().getSimpleName();
            if (ignored.contains(packetName))
                return;

            String playerName = getPlayerName(handler);
            if (playerName != null) {
                stats.computeIfAbsent(playerName, k -> new PlayerStats())
                        .received.computeIfAbsent(packet.getClass().getSimpleName(), k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
        });

        // Listener for sent packets (Outbound)
        PacketAdapters.registerOutbound((PacketHandler handler, Packet packet) -> {
            var packetName = packet.getClass().getSimpleName();
            if (ignored.contains(packetName))
                return;

            String playerName = getPlayerName(handler);
            if (playerName != null) {
                stats.computeIfAbsent(playerName, k -> new PlayerStats())
                        .sent.computeIfAbsent(packet.getClass().getSimpleName(), k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
        });

        // Schedule logging every 3 seconds
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            if (stats.isEmpty()) return;

            for (Map.Entry<String, PlayerStats> entry : stats.entrySet()) {
                String player = entry.getKey();
                PlayerStats pStats = entry.getValue();

                StringBuilder sb = new StringBuilder();

                // Build Sent string
                List<String> sentLogs = new ArrayList<>();
                pStats.sent.forEach((type, atomic) -> {
                    int count = atomic.getAndSet(0);
                    if (count > 0) {
                        sentLogs.add(type + " x" + count);
                    }
                });

                if (!sentLogs.isEmpty()) {
                    sb.append("Sent ").append(String.join(", ", sentLogs));
                }

                // Build Received string
                List<String> recvLogs = new ArrayList<>();
                pStats.received.forEach((type, atomic) -> {
                    int count = atomic.getAndSet(0);
                    if (count > 0) {
                        recvLogs.add(type + " x" + count);
                    }
                });

                if (!recvLogs.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append("Received ").append(String.join(", ", recvLogs));
                }

                LOGGER.atInfo().log("To " + player + ":\n" + sb + "\n");
            }
        }, 3, 3, TimeUnit.SECONDS);
    }
    public static void registerStreams() {
        PacketAdapters.registerInbound(PlayerPacketTracker.getWatcher(registeredIn));
        PacketAdapters.registerOutbound(PlayerPacketTracker.getWatcher(registeredOut));
    }
    private static PacketWatcher getWatcher(Map<String, BiConsumer<GamePacketHandler, ? extends Packet>> map) {
        return (handler, packet) -> {
            if (handler instanceof GamePacketHandler gpHandler) {
                String packetName = packet.getClass().getSimpleName();

                BiConsumer<GamePacketHandler, ? extends Packet> function = map.get(packetName);
                if (function != null)
                    function.accept(gpHandler, Adapter.cast(packet));
            }
        };
    }

    private static Map<String, BiConsumer<GamePacketHandler, ? extends Packet>> registeredIn = new HashMap<>();
    private static Map<String, BiConsumer<GamePacketHandler, ? extends Packet>> registeredOut = new HashMap<>();
    public static <T extends Packet> void registerHandlerIn(Class<T> clazz, BiConsumer<GamePacketHandler, T> function) {
        PlayerPacketTracker.registeredIn.put(clazz.getSimpleName(), function);
    }
    public static <T extends Packet> void registerHandlerOut(Class<T> clazz, BiConsumer<GamePacketHandler, T> function) {
        PlayerPacketTracker.registeredOut.put(clazz.getSimpleName(), function);
    }


    static {
        ignored.add("Ping");
        ignored.add("Pong");
        ignored.add("EntityUpdates");
        ignored.add("CachedPacket");
        ignored.add("UpdateTime");
        ignored.add("PlayAnimation");
        ignored.add("SetEntitySeed");
        ignored.add("UpdateServerPlayerListPing");
    }
}
