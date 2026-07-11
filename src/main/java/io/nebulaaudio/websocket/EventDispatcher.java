package io.nebulaaudio.websocket;

import io.javalin.websocket.WsContext;
import io.nebulaaudio.source.AudioTrack;
import io.nebulaaudio.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks connected sessions (one WsContext per sessionId, matching Lavalink's
 * one-websocket-per-node-connection model) and serializes/sends events to
 * the right one. AudioPlayer instances call into this rather than touching
 * WebSocket internals directly.
 */
public class EventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    private final Map<String, WsContext> sessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, WsContext ctx) {
        sessions.put(sessionId, ctx);
        log.info("Session {} connected", sessionId);
    }

    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session {} disconnected", sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    private void send(String sessionId, Object payload) {
        WsContext ctx = sessions.get(sessionId);
        if (ctx == null) {
            log.debug("Dropping event for unknown/disconnected session {}", sessionId);
            return;
        }
        try {
            ctx.send(Json.toJson(payload));
        } catch (Exception e) {
            log.warn("Failed to send event to session {}: {}", sessionId, e.getMessage());
        }
    }

    public void dispatchReady(String sessionId, boolean resumed) {
        Map<String, Object> payload = Map.of("op", "ready", "resumed", resumed, "sessionId", sessionId);
        send(sessionId, payload);
    }

    public void dispatchTrackStart(String sessionId, String guildId, AudioTrack track) {
        send(sessionId, new PlayerEvents.TrackStartEvent(guildId, track));
    }

    public void dispatchTrackEnd(String sessionId, String guildId, AudioTrack track, String reason) {
        send(sessionId, new PlayerEvents.TrackEndEvent(guildId, track, reason));
    }

    public void dispatchTrackException(String sessionId, String guildId, AudioTrack track, String message, String severity, String cause) {
        send(sessionId, new PlayerEvents.TrackExceptionEvent(guildId, track, message, severity, cause));
    }

    public void dispatchTrackStuck(String sessionId, String guildId, AudioTrack track, long thresholdMs) {
        send(sessionId, new PlayerEvents.TrackStuckEvent(guildId, track, thresholdMs));
    }

    public void dispatchPlayerUpdate(String sessionId, String guildId, long positionMs, boolean connected) {
        send(sessionId, new PlayerEvents.PlayerUpdateEvent(guildId, positionMs, connected));
    }
}
