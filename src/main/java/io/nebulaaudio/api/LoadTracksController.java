package io.nebulaaudio.api;

import io.javalin.http.Context;
import io.nebulaaudio.source.LoadResult;
import io.nebulaaudio.source.SourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Handles GET /v4/loadtracks?identifier=... and GET /v4/info.
 */
public class LoadTracksController {
    private static final Logger log = LoggerFactory.getLogger(LoadTracksController.class);

    private final SourceManager sourceManager;

    public LoadTracksController(SourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

    public void loadTracks(Context ctx) {
        String identifier = ctx.queryParam("identifier");
        if (identifier == null || identifier.isBlank()) {
            ctx.status(400).json(Map.of("error", "Missing required query parameter 'identifier'"));
            return;
        }

        log.debug("loadtracks: {}", identifier);
        LoadResult result = sourceManager.loadItem(identifier);
        ctx.json(result);
    }

    public void info(Context ctx) {
        Map<String, Object> info = Map.of(
                "version", Map.of(
                        "semver", "1.0.0",
                        "major", 1,
                        "minor", 0,
                        "patch", 0
                ),
                "buildTime", System.currentTimeMillis(),
                "jvm", System.getProperty("java.version"),
                "sourceManagers", sourceManager.getManagers().stream().map(m -> m.getName()).toList(),
                "filters", List.of("equalizer", "bassboost", "nightcore", "timescale", "karaoke", "tremolo", "vibrato", "rotation", "lowpass"),
                "plugins", List.of()
        );
        ctx.json(info);
    }
}
