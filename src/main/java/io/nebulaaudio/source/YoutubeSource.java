package io.nebulaaudio.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOT YET IMPLEMENTED.
 *
 * A real YouTube source requires reverse-engineering YouTube's player
 * response / signature-cipher / PoToken flow to obtain a direct-playable
 * stream URL, and that logic breaks and needs updating frequently (this is
 * why Lavalink itself moved YouTube support out into a separately-maintained,
 * frequently-updated plugin — youtube-source — rather than keeping it in
 * core). Shipping a fake implementation here would silently fail at runtime
 * and be worse than being explicit about the gap.
 *
 * Extension points if/when you wire in a real extractor:
 *   1. Resolve `ytsearch:<query>` or a youtube.com/watch?v= URL to a video ID.
 *   2. Call an extractor (e.g. shell out to yt-dlp, or port youtube-source's
 *      Java client) to get: title, author, duration, and a direct googlevideo
 *      stream URL (or an itag-specific audio stream).
 *   3. Set AudioTrack.playbackUri to that direct URL — FFmpegBridge handles
 *      the rest unchanged, since it just execs ffmpeg -i <uri>.
 *   4. googlevideo URLs expire (~6hrs) and are IP-bound; if you cache tracks,
 *      re-resolve playbackUri at play time, not at load time.
 *
 * Fastest path to "actually works": shell out to yt-dlp (Python) as a
 * subprocess similar to FFmpegBridge, using `yt-dlp -f bestaudio -g <url>`
 * to get a direct stream URL, then hand that URL to ffmpeg as normal. This
 * avoids reimplementing YouTube's cipher logic in Java entirely, at the cost
 * of a yt-dlp dependency + its own maintenance burden (also updates often).
 */
public class YoutubeSource implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(YoutubeSource.class);

    @Override
    public String getName() {
        return "youtube";
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith("ytsearch:")
                || identifier.contains("youtube.com/")
                || identifier.contains("youtu.be/");
    }

    @Override
    public LoadResult loadItem(String identifier) {
        log.warn("YouTube source was invoked for '{}' but is not implemented — see YoutubeSource.java for extension notes", identifier);
        return LoadResult.error(
                "YouTube source is not implemented in this build. See YoutubeSource.java for extension notes " +
                        "(recommended path: shell out to yt-dlp for stream resolution).",
                "common",
                "not_implemented");
    }
}
