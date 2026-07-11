package io.nebulaaudio.api;

import io.javalin.http.Context;
import io.nebulaaudio.player.PlayerManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;

/**
 * Handles GET /v4/stats — node-wide statistics, matching Lavalink's stats
 * payload shape (players, playingPlayers, uptime, memory, cpu, frameStats).
 */
public class SessionController {
    private final PlayerManager playerManager;
    private final long startTime = System.currentTimeMillis();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    public SessionController(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void stats(Context ctx) {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long total = runtime.totalMemory();
        long max = runtime.maxMemory();

        double systemLoad = osBean.getSystemLoadAverage();
        int cores = osBean.getAvailableProcessors();

        int totalFramesSent = playerManager.all().values().stream()
                .mapToInt(p -> p.getCurrentTrack() != null ? 1 : 0)
                .sum();
        int totalDeficit = playerManager.all().values().stream()
                .mapToInt(io.nebulaaudio.player.AudioPlayer::getFrameDeficit)
                .sum();

        Map<String, Object> stats = Map.of(
                "players", playerManager.totalPlayers(),
                "playingPlayers", (int) playerManager.playingPlayers(),
                "uptime", System.currentTimeMillis() - startTime,
                "memory", Map.of(
                        "free", max - used,
                        "used", used,
                        "allocated", total,
                        "reservable", max
                ),
                "cpu", Map.of(
                        "cores", cores,
                        "systemLoad", systemLoad < 0 ? 0.0 : systemLoad,
                        "lavalinkLoad", 0.0
                ),
                "frameStats", Map.of(
                        "sent", totalFramesSent,
                        "nulled", 0,
                        "deficit", totalDeficit
                )
        );

        ctx.json(stats);
    }
}
