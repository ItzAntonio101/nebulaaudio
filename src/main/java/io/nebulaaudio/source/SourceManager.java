package io.nebulaaudio.source;

import io.nebulaaudio.config.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of AudioSourceManagers. Picks the right manager for an
 * identifier (search prefix or URL) based on nebula.yml's sources.* toggles,
 * and delegates loadItem to it. Mirrors Lavalink's AudioPlayerManager routing.
 */
public class SourceManager {
    private static final Logger log = LoggerFactory.getLogger(SourceManager.class);

    private final List<AudioSourceManager> managers = new ArrayList<>();

    public SourceManager(SourceConfig config) {
        if (config.isHttp()) managers.add(new HttpSource());
        if (config.isLocal()) managers.add(new LocalFileSource());
        if (config.isYoutube()) managers.add(new YoutubeSource());
        if (config.isSoundcloud()) managers.add(new SoundCloudSource());
        if (config.isDeezer()) managers.add(new DeezerSource());
        if (config.isSpotify()) managers.add(new SpotifySource());

        log.info("Registered sources: {}", managers.stream().map(AudioSourceManager::getName).toList());
    }

    public LoadResult loadItem(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return LoadResult.error("No identifier provided", "common", "bad_request");
        }

        for (AudioSourceManager manager : managers) {
            if (manager.canHandle(identifier)) {
                try {
                    return manager.loadItem(identifier);
                } catch (Exception e) {
                    log.error("Source '{}' threw while loading '{}'", manager.getName(), identifier, e);
                    return LoadResult.error("Internal error in source " + manager.getName() + ": " + e.getMessage(),
                            "fault", "exception");
                }
            }
        }

        return LoadResult.empty();
    }

    public List<AudioSourceManager> getManagers() {
        return managers;
    }
}
