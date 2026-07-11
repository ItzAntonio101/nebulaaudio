package io.nebulaaudio.player;

import io.nebulaaudio.config.NebulaConfig;
import io.nebulaaudio.websocket.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Owns the lifecycle of all AudioPlayer instances, keyed by "sessionId:guildId"
 * (a session can have many guild players; Lavalink keys the same way).
 */
public class PlayerManager {
    private static final Logger log = LoggerFactory.getLogger(PlayerManager.class);

    private final Map<String, AudioPlayer> players = new ConcurrentHashMap<>();
    private final EventDispatcher dispatcher;
    private final NebulaConfig config;
    private final ScheduledExecutorService executor;

    public PlayerManager(EventDispatcher dispatcher, NebulaConfig config) {
        this.dispatcher = dispatcher;
        this.config = config;
        // Sized generously: each player uses 2 recurring tasks (send loop @20ms, update loop @5s).
        this.executor = Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
    }

    private static String key(String sessionId, String guildId) {
        return sessionId + ":" + guildId;
    }

    public AudioPlayer getOrCreate(String sessionId, String guildId) {
        return players.computeIfAbsent(key(sessionId, guildId), k -> {
            log.info("Creating player for session={} guild={}", sessionId, guildId);
            return new AudioPlayer(guildId, sessionId, dispatcher, executor,
                    config.getPlayer().getDefaultVolume(), config.getPlayer().getOpusQuality());
        });
    }

    public AudioPlayer get(String sessionId, String guildId) {
        return players.get(key(sessionId, guildId));
    }

    public void destroy(String sessionId, String guildId) {
        AudioPlayer player = players.remove(key(sessionId, guildId));
        if (player != null) {
            player.destroy();
            log.info("Destroyed player for session={} guild={}", sessionId, guildId);
        }
    }

    public void destroyAllForSession(String sessionId) {
        players.keySet().stream()
                .filter(k -> k.startsWith(sessionId + ":"))
                .toList()
                .forEach(k -> {
                    AudioPlayer p = players.remove(k);
                    if (p != null) p.destroy();
                });
    }

    public int totalPlayers() {
        return players.size();
    }

    public long playingPlayers() {
        return players.values().stream().filter(AudioPlayer::isPlaying).count();
    }

    public Map<String, AudioPlayer> all() {
        return players;
    }
}
