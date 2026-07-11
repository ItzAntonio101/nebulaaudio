package io.nebulaaudio.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps an ffmpeg subprocess that decodes an input (URL or local file path)
 * into raw signed 16-bit little-endian PCM, stereo, 48kHz — the format the
 * Opus encoder expects. This mirrors how Lavalink/LavaPlayer shell out to
 * ffmpeg rather than re-implementing container/codec demuxing in Java.
 *
 * Requires an `ffmpeg` binary on PATH. On HiddenCloud/Pterodactyl-style
 * containers without root, ffmpeg typically needs to be vendored as a static
 * binary and its path set via the FFMPEG_PATH environment variable.
 */
public class FFmpegBridge {
    private static final Logger log = LoggerFactory.getLogger(FFmpegBridge.class);

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNELS = 2;

    private final String ffmpegPath;
    private Process process;

    public FFmpegBridge() {
        String envPath = System.getenv("FFMPEG_PATH");
        this.ffmpegPath = (envPath != null && !envPath.isBlank()) ? envPath : "ffmpeg";
    }

    /**
     * Starts decoding the given input starting at the given offset (milliseconds).
     * Returns an InputStream of raw PCM bytes (s16le, 48kHz, stereo).
     */
    public InputStream start(String inputUri, long startPositionMs) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-hide_banner");
        cmd.add("-loglevel");
        cmd.add("warning");

        if (startPositionMs > 0) {
            cmd.add("-ss");
            cmd.add(String.valueOf(startPositionMs / 1000.0));
        }

        cmd.add("-i");
        cmd.add(inputUri);

        // Output: raw PCM, 48kHz stereo, to stdout
        cmd.add("-f");
        cmd.add("s16le");
        cmd.add("-ar");
        cmd.add(String.valueOf(SAMPLE_RATE));
        cmd.add("-ac");
        cmd.add(String.valueOf(CHANNELS));
        cmd.add("-vn");
        cmd.add("pipe:1");

        log.debug("Starting ffmpeg: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        this.process = pb.start();

        // Drain stderr in the background so ffmpeg doesn't block on a full pipe,
        // and log anything interesting (e.g. "404 Not Found", codec errors).
        Thread stderrDrain = new Thread(() -> {
            try (InputStream err = process.getErrorStream()) {
                byte[] buf = new byte[4096];
                int n;
                StringBuilder sb = new StringBuilder();
                while ((n = err.read(buf)) != -1) {
                    sb.append(new String(buf, 0, n));
                }
                if (!sb.isEmpty()) {
                    log.debug("ffmpeg stderr: {}", sb);
                }
            } catch (IOException ignored) {
                // process torn down; nothing to do
            }
        }, "ffmpeg-stderr-drain");
        stderrDrain.setDaemon(true);
        stderrDrain.start();

        return new BufferedInputStream(process.getInputStream(), 1 << 16);
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }
}
