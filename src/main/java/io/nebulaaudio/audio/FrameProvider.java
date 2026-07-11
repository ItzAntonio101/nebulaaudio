package io.nebulaaudio.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads raw PCM from an FFmpegBridge's stdout, slices it into 20ms frames,
 * encodes each to Opus, and pushes the result into a bounded queue that the
 * player/scheduler drains at playback rate. Runs its own decode thread so
 * ffmpeg I/O never blocks the frame-send loop.
 *
 * Also tracks "frame deficit" — frames that were expected but couldn't be
 * produced in time (used in /v4/stats reporting, mirroring Lavalink's
 * frameStats.deficit).
 */
public class FrameProvider {
    private static final Logger log = LoggerFactory.getLogger(FrameProvider.class);

    private static final int PCM_FRAME_BYTES =
            OpusEncoder.FRAME_SIZE_SAMPLES * FFmpegBridge.CHANNELS * 2; // 2 bytes per s16 sample

    private final BlockingQueue<byte[]> opusQueue = new ArrayBlockingQueue<>(50); // ~1s of audio buffered
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger framesSent = new AtomicInteger(0);
    private final AtomicInteger framesDeficit = new AtomicInteger(0);
    private final AtomicLong positionMs = new AtomicLong(0);

    private final OpusEncoder encoder;
    private Thread decodeThread;

    public FrameProvider(OpusEncoder encoder) {
        this.encoder = encoder;
    }

    public void start(InputStream pcmStream) {
        if (running.compareAndSet(false, true)) {
            decodeThread = new Thread(() -> decodeLoop(pcmStream), "frame-provider-decode");
            decodeThread.setDaemon(true);
            decodeThread.start();
        }
    }

    private void decodeLoop(InputStream pcmStream) {
        byte[] pcmBytes = new byte[PCM_FRAME_BYTES];
        short[] pcmShorts = new short[OpusEncoder.FRAME_SIZE_SAMPLES * FFmpegBridge.CHANNELS];
        byte[] opusOut = new byte[4000]; // max Opus packet size margin

        try {
            while (running.get()) {
                int read = readFully(pcmStream, pcmBytes);
                if (read <= 0) {
                    break; // EOF: track finished
                }
                if (read < pcmBytes.length) {
                    // Partial final frame — zero-pad the remainder (silence) so
                    // the encoder always gets a full frame.
                    java.util.Arrays.fill(pcmBytes, read, pcmBytes.length, (byte) 0);
                }

                bytesToShorts(pcmBytes, pcmShorts);
                int encodedLen = encoder.encode(pcmShorts, opusOut);
                if (encodedLen > 0) {
                    byte[] packet = java.util.Arrays.copyOf(opusOut, encodedLen);
                    boolean offered = opusQueue.offer(packet);
                    if (!offered) {
                        framesDeficit.incrementAndGet();
                        log.debug("Opus queue full, dropping frame (deficit++)");
                    } else {
                        framesSent.incrementAndGet();
                    }
                    positionMs.addAndGet(OpusEncoder.FRAME_SIZE_MS);
                } else {
                    framesDeficit.incrementAndGet();
                }
            }
        } catch (IOException e) {
            log.debug("PCM stream closed: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int n = in.read(buf, total, buf.length - total);
            if (n == -1) {
                return total; // EOF, possibly partial
            }
            total += n;
        }
        return total;
    }

    private static void bytesToShorts(byte[] bytes, short[] shorts) {
        for (int i = 0; i < shorts.length; i++) {
            int lo = bytes[i * 2] & 0xFF;
            int hi = bytes[i * 2 + 1];
            shorts[i] = (short) ((hi << 8) | lo);
        }
    }

    /** Blocks up to timeoutMs waiting for the next Opus frame; null if none available. */
    public byte[] poll(long timeoutMs) {
        try {
            return opusQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void stop() {
        running.set(false);
        opusQueue.clear();
        if (decodeThread != null) {
            decodeThread.interrupt();
        }
    }

    public long getPositionMs() {
        return positionMs.get();
    }

    public void setPositionMs(long ms) {
        positionMs.set(ms);
    }

    public int getFramesSent() {
        return framesSent.get();
    }

    public int getFramesDeficit() {
        return framesDeficit.get();
    }

    public boolean isRunning() {
        return running.get();
    }
}
