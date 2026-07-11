package io.nebulaaudio.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOT YET IMPLEMENTED (and only ever metadata resolution — by design).
 *
 * Spotify does not allow direct audio playback of full tracks outside their
 * own client — Spotify streams are DRM-protected and there is no legitimate
 * server-side "play this Spotify track's audio" path. The standard and
 * legitimate pattern (matching Lavalink's LavaSrc plugin) is:
 *   1. Use the Spotify Web API (Client Credentials flow — app-only auth,
 *      no user login needed) to resolve spsearch:<query> or a spotify: URI
 *      into track metadata (title, artist(s), duration, ISRC, artwork).
 *   2. Do NOT attempt to fetch Spotify's actual audio stream.
 *   3. Use the resolved title/artist (or ISRC, if you also implement Deezer
 *      metadata lookup) to search another playable source — YouTube or
 *      SoundCloud — and play that instead. This is exactly what Lavalink/
 *      LavaSrc's "Spotify" source does under the hood.
 *
 * Extension points:
 *   1. Register a Spotify Developer app, get client_id/client_secret.
 *   2. POST to accounts.spotify.com/api/token (client_credentials grant).
 *   3. GET api.spotify.com/v1/search or /v1/tracks/{id} for metadata.
 *   4. Feed metadata into another AudioSourceManager for actual playback.
 */
public class SpotifySource implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(SpotifySource.class);

    @Override
    public String getName() {
        return "spotify";
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith("spsearch:") || identifier.contains("spotify.com/") || identifier.startsWith("spotify:");
    }

    @Override
    public LoadResult loadItem(String identifier) {
        log.warn("Spotify source was invoked for '{}' but is not implemented — see SpotifySource.java", identifier);
        return LoadResult.error(
                "Spotify source is not implemented in this build. Spotify only ever supports metadata resolution " +
                        "(no direct playback is possible/legitimate) — see SpotifySource.java for the metadata+handoff pattern.",
                "common",
                "not_implemented");
    }
}
