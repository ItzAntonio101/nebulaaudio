package io.nebulaaudio.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOT YET IMPLEMENTED.
 *
 * Deezer's full-track streams are DRM-protected and require an authenticated
 * user session plus a decryption step — not something a server-side node
 * should be doing without a legitimate Deezer partner agreement. What's
 * realistic without that: resolve dzsearch:<query> against Deezer's public
 * (unauthenticated) metadata API to get title/artist/artwork/ISRC, and either
 * (a) play Deezer's 30-second preview MP3 (public, no auth needed), or
 * (b) use the resolved ISRC to hand off to another source (e.g. search
 * YouTube/SoundCloud for the same ISRC/title+artist) for full playback —
 * this is what Lavalink's Deezer plugins typically do.
 *
 * Extension points:
 *   1. GET https://api.deezer.com/search?q=<query> for metadata (no auth).
 *   2. Either stream the `preview` field directly (30s clip, always legal to
 *      access), or use metadata to cross-resolve via another AudioSourceManager.
 */
public class DeezerSource implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(DeezerSource.class);

    @Override
    public String getName() {
        return "deezer";
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith("dzsearch:") || identifier.contains("deezer.com/");
    }

    @Override
    public LoadResult loadItem(String identifier) {
        log.warn("Deezer source was invoked for '{}' but is not implemented — see DeezerSource.java", identifier);
        return LoadResult.error(
                "Deezer source is not implemented in this build. See DeezerSource.java for extension notes " +
                        "(public metadata + 30s preview is the realistic no-auth path).",
                "common",
                "not_implemented");
    }
}
