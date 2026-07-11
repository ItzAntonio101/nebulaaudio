package io.nebulaaudio.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOT YET IMPLEMENTED.
 *
 * SoundCloud's public API requires a client_id that SoundCloud rotates and
 * doesn't officially issue for third-party server use — Lavalink's
 * soundcloud extractor scrapes a current client_id from SoundCloud's own web
 * app bundle at startup. That's a legitimate but fragile approach that needs
 * upkeep as SoundCloud changes their frontend build.
 *
 * Extension points:
 *   1. On startup (or lazily on first use), fetch soundcloud.com's homepage
 *      HTML, find the referenced JS bundle URLs, and regex out the
 *      client_id embedded in one of them.
 *   2. Use that client_id against SoundCloud's API (api-v2.soundcloud.com)
 *      to resolve scsearch:<query> to track metadata + a stream URL
 *      (progressive or HLS `.m3u8` — ffmpeg can play HLS URLs directly).
 *   3. Cache the client_id; refresh it when a request starts failing with 401.
 */
public class SoundCloudSource implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudSource.class);

    @Override
    public String getName() {
        return "soundcloud";
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith("scsearch:") || identifier.contains("soundcloud.com/");
    }

    @Override
    public LoadResult loadItem(String identifier) {
        log.warn("SoundCloud source was invoked for '{}' but is not implemented — see SoundCloudSource.java", identifier);
        return LoadResult.error(
                "SoundCloud source is not implemented in this build. See SoundCloudSource.java for extension notes.",
                "common",
                "not_implemented");
    }
}
