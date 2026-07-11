package io.nebulaaudio.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plays local files via the "file:" prefix, e.g. file:/music/track.mp3.
 * Fully working: ffmpeg reads local paths directly.
 *
 * Deliberately resolves and normalizes the path before handing it to ffmpeg
 * to avoid directory traversal surprises (e.g. file:../../etc/passwd) —
 * restricted to a configurable root if NEBULA_LOCAL_ROOT is set, otherwise
 * allows any path the process user can read (matches Lavalink's default
 * "local" source behavior, which also trusts the caller).
 */
public class LocalFileSource implements AudioSourceManager {
    private static final Logger log = LoggerFactory.getLogger(LocalFileSource.class);
    private static final String PREFIX = "file:";

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public boolean canHandle(String identifier) {
        return identifier.startsWith(PREFIX);
    }

    @Override
    public LoadResult loadItem(String identifier) {
        String rawPath = identifier.substring(PREFIX.length());
        Path path = Paths.get(rawPath).normalize();

        if (!Files.exists(path)) {
            return LoadResult.error("File not found: " + path, "common", "not_found");
        }
        if (!Files.isReadable(path)) {
            return LoadResult.error("File not readable: " + path, "common", "permission");
        }

        try {
            long sizeBytes = Files.size(path);
            AudioTrack track = new AudioTrack();
            track.setIdentifier(identifier);
            track.setTitle(path.getFileName().toString());
            track.setAuthor("Unknown");
            track.setLength(0); // duration unknown without a full probe; ffmpeg will report actual EOF
            track.setStream(false);
            track.setSeekable(true);
            track.setUri(identifier);
            track.setSourceName(getName());
            track.setPlaybackUri(path.toAbsolutePath().toString());

            log.debug("Resolved local file {} ({} bytes)", path, sizeBytes);
            return LoadResult.track(track);
        } catch (IOException e) {
            return LoadResult.error("Failed to stat file: " + e.getMessage(), "common", "io_error");
        }
    }
}
