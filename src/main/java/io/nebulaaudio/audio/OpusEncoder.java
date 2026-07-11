package io.nebulaaudio.audio;

import org.concentus.OpusApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes raw PCM (s16le, 48kHz, stereo) frames into Opus packets.
 *
 * Uses Concentus, a pure-Java Opus port, instead of JNI bindings to a native
 * libopus — this avoids needing to compile/ship native .so files per platform,
 * which matters a lot on a constrained, no-root Pterodactyl container where
 * you can't apt-get install libopus-dev. Trade-off: somewhat higher CPU cost
 * than native libopus, acceptable for moderate concurrent player counts.
 */
public class OpusEncoder {
    private static final Logger log = LoggerFactory.getLogger(OpusEncoder.class);

    public static final int FRAME_SIZE_MS = 20;
    public static final int FRAME_SIZE_SAMPLES = FFmpegBridge.SAMPLE_RATE / 1000 * FRAME_SIZE_MS; // 960 samples/channel

    private final org.concentus.OpusEncoder encoder;
    private final int bitrate;

    private OpusEncoder(int bitrateBps) {
        this.bitrate = bitrateBps;
        try {
            this.encoder = new org.concentus.OpusEncoder(FFmpegBridge.SAMPLE_RATE, FFmpegBridge.CHANNELS, OpusApplication.OPUS_APPLICATION_AUDIO);
            this.encoder.setBitrate(bitrateBps);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Opus encoder", e);
        }
    }

    /** Creates an encoder from a Lavalink-style 0-10 opus-quality knob (nebula.yml's player.opus-quality). */
    public static OpusEncoder forQuality(int quality) {
        return new OpusEncoder(qualityToBitrate(quality));
    }

    /** Creates an encoder from an explicit bitrate in bits per second. */
    public static OpusEncoder forBitrate(int bitrateBps) {
        return new OpusEncoder(bitrateBps);
    }

    private static int qualityToBitrate(int quality) {
        // Map a Lavalink-style 0-10 "opus-quality" knob onto a bitrate range.
        int clamped = Math.max(0, Math.min(10, quality));
        return 32_000 + (clamped * 12_800); // 32kbps..160kbps
    }

    /**
     * Encodes one 20ms frame of interleaved stereo s16 PCM samples into Opus.
     * pcm.length must equal FRAME_SIZE_SAMPLES * CHANNELS.
     * Returns the number of valid bytes written into out.
     */
    public int encode(short[] pcm, byte[] out) {
        try {
            return encoder.encode(pcm, 0, FRAME_SIZE_SAMPLES, out, 0, out.length);
        } catch (Exception e) {
            log.warn("Opus encode failure: {}", e.getMessage());
            return 0;
        }
    }

    public int getBitrate() {
        return bitrate;
    }
}
