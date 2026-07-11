package io.nebulaaudio.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import io.nebulaaudio.player.AudioPlayer;
import io.nebulaaudio.player.Filters;
import io.nebulaaudio.player.PlayerManager;
import io.nebulaaudio.source.AudioTrack;
import io.nebulaaudio.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles PATCH/DELETE /v4/sessions/{sessionId}/players/{guildId}, matching
 * Lavalink v4's player-update contract: a partial JSON body where each
 * top-level field (track, position, volume, paused, filters) is applied
 * independently if present.
 */
public class PlayerController {
    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerManager playerManager;

    public PlayerController(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void patch(Context ctx) {
        String sessionId = ctx.pathParam("sessionId");
        String guildId = ctx.pathParam("guildId");
        AudioPlayer player = playerManager.getOrCreate(sessionId, guildId);

        JsonNode body;
        try {
            body = Json.mapper().readTree(ctx.body());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid JSON body: " + e.getMessage()));
            return;
        }

        if (body.has("track")) {
            JsonNode trackNode = body.get("track");
            if (trackNode.isNull()) {
                player.stop();
            } else if (trackNode.has("encoded") && !trackNode.get("encoded").isNull()) {
                try {
                    AudioTrack track = AudioTrack.decode(trackNode.get("encoded").asText());
                    player.play(track);
                } catch (Exception e) {
                    ctx.status(400).json(Map.of("error", "Invalid encoded track: " + e.getMessage()));
                    return;
                }
            }
        }

        if (body.has("position")) {
            player.seek(body.get("position").asLong());
        }

        if (body.has("paused")) {
            player.setPaused(body.get("paused").asBoolean());
        }

        if (body.has("volume")) {
            player.setVolume(body.get("volume").asInt());
        }

        if (body.has("filters")) {
            try {
                Filters filters = Json.mapper().treeToValue(body.get("filters"), Filters.class);
                player.setFilters(filters);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Invalid filters: " + e.getMessage()));
                return;
            }
        }

        ctx.json(toPlayerJson(guildId, player));
    }

    public void delete(Context ctx) {
        String sessionId = ctx.pathParam("sessionId");
        String guildId = ctx.pathParam("guildId");
        playerManager.destroy(sessionId, guildId);
        ctx.status(204);
    }

    public void get(Context ctx) {
        String sessionId = ctx.pathParam("sessionId");
        String guildId = ctx.pathParam("guildId");
        AudioPlayer player = playerManager.get(sessionId, guildId);
        if (player == null) {
            ctx.status(404).json(Map.of("error", "No player for guild " + guildId));
            return;
        }
        ctx.json(toPlayerJson(guildId, player));
    }

    public void listAll(Context ctx) {
        String sessionId = ctx.pathParam("sessionId");
        var result = playerManager.all().entrySet().stream()
                .filter(e -> e.getKey().startsWith(sessionId + ":"))
                .map(e -> toPlayerJson(e.getKey().substring(sessionId.length() + 1), e.getValue()))
                .toList();
        ctx.json(result);
    }

    private Map<String, Object> toPlayerJson(String guildId, AudioPlayer player) {
        AudioTrack track = player.getCurrentTrack();
        return Map.of(
                "guildId", guildId,
                "track", track == null ? Map.of() : Map.of("encoded", track.getEncoded(), "info", track.info()),
                "volume", player.getVolume(),
                "paused", player.isPaused(),
                "state", Map.of(
                        "time", System.currentTimeMillis(),
                        "position", player.getPositionMs(),
                        "connected", player.isPlaying(),
                        "ping", -1
                ),
                "filters", player.getFilters()
        );
    }
}
