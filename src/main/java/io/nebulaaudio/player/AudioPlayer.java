package io.nebulaaudio.player;

import io.nebulaaudio.audio.FFmpegBridge;
import io.nebulaaudio.audio.FrameProvider;
import io.nebulaaudio.audio.OpusEncoder;
import io.nebulaaudio.source.AudioTrack;
import io.nebulaaudio.websocket.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One player per guild. Owns the current track's ffmpeg process + frame
 * provider, applies volume, and drives the send loop that pulls Opus frames
 * at real-time (20ms) pace. Actual voice-gateway transmission (sending the
 * Opus packets to Discord's UDP voice socket) is intentionally out of scope
 * here — NebulaAudio, like Lavalink, expects the calling bot library to
 * supply voice-server connection info via the session/player PATCH endpoint,
 * and a full Discord voice UDP/RTP implementation is its own substantial
 * subsystem. sendFrame() below is the seam where that would plug in.
 */
public class AudioPlayer {
    private static final Logger log = LoggerFactory.getLogger(AudioPlayer.class);
    private static final long STUCK_THRESHOLD_MS = 10_000;

    private final String guildId;
    private final String sessionId;
    private final EventDispatcher dispatcher;
    private final TrackScheduler scheduler = new TrackScheduler();
    private final ScheduledExecutorService executor;
    private final int opusQuality;

    private final AtomicReference<AudioTrack> currentTrack = new AtomicReference<>();
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private volatile int volume = 100;
    private volatile Filters filters = new Filters();

    private FFmpegBridge ffmpeg;
    private FrameProvider frameProvider;
    private ScheduledFuture<?> sendLoopFuture;
    private ScheduledFuture<?> updateLoopFuture;
    private long lastFrameAt = System.currentTimeMillis();

    public AudioPlayer(String guildId, String sessionId, EventDispatcher dispatcher,
                        ScheduledExecutorService executor, int defaultVolume, int opusQuality) {
        this.guildId = guildId;
        this.sessionId = sessionId;
        this.dispatcher = dispatcher;
        this.executor = executor;
        this.volume = defaultVolume;
        this.opusQuality = opusQuality;

        // Periodic playerUpdate events, matching Lavalink's ~5s cadence.
        this.updateLoopFuture = executor.scheduleAtFixedRate(this::emitPlayerUpdate, 5, 5, TimeUnit.SECONDS);
    }

    public synchronized void play(AudioTrack track) {
        stopInternal("REPLACED");
        startTrack(track);
    }

    public synchronized void queue(AudioTrack track) {
        if (currentTrack.get() == null) {
            startTrack(track);
        } else {
            scheduler.enqueue(track);
        }
    }

    private void startTrack(AudioTrack track) {
        try {
            ffmpeg = new FFmpegBridge();
            InputStream pcm = ffmpeg.start(track.getPlaybackUri(), 0);

            OpusEncoder encoder = OpusEncoder.forQuality(opusQuality);
            frameProvider = new FrameProvider(encoder);
            frameProvider.start(pcm);

            currentTrack.set(track);
            playing.set(true);
            paused.set(false);
            lastFrameAt = System.currentTimeMillis();

            dispatcher.dispatchTrackStart(sessionId, guildId, track);
            startSendLoop();
            log.info("[{}] Started track: {}", guildId, track.getTitle());
        } catch (Exception e) {
            log.error("[{}] Failed to start track {}: {}", guildId, track.getTitle(), e.getMessage(), e);
            dispatcher.dispatchTrackException(sessionId, guildId, track, e.getMessage(), "fault", "ffmpeg_start_failed");
            advanceQueue("LOAD_FAILED");
        }
    }

    private void startSendLoop() {
        if (sendLoopFuture != null) {
            sendLoopFuture.cancel(false);
        }
        sendLoopFuture = executor.scheduleAtFixedRate(this::sendLoopTick, 0, OpusEncoder.FRAME_SIZE_MS, TimeUnit.MILLISECONDS);
    }

    private void sendLoopTick() {
        if (!playing.get() || paused.get() || frameProvider == null) {
            return;
        }

        byte[] frame = frameProvider.poll(OpusEncoder.FRAME_SIZE_MS);
        if (frame != null) {
            lastFrameAt = System.currentTimeMillis();
            sendFrame(applyVolume(frame));
            return;
        }

        // No frame available: either the track ended, or ffmpeg is stalled.
        if (!frameProvider.isRunning() && (ffmpeg == null || !ffmpeg.isAlive())) {
            AudioTrack finished = currentTrack.get();
            log.info("[{}] Track finished: {}", guildId, finished != null ? finished.getTitle() : "unknown");
            advanceQueue("FINISHED");
            return;
        }

        long stalledFor = System.currentTimeMillis() - lastFrameAt;
        if (stalledFor > STUCK_THRESHOLD_MS) {
            AudioTrack stuck = currentTrack.get();
            log.warn("[{}] Track stuck for {}ms: {}", guildId, stalledFor, stuck != null ? stuck.getTitle() : "unknown");
            dispatcher.dispatchTrackStuck(sessionId, guildId, stuck, stalledFor);
            advanceQueue("STOPPED");
        }
    }

    /**
     * Seam for actually transmitting an Opus frame to Discord's voice UDP
     * socket. NebulaAudio does not implement Discord voice-gateway/UDP/RTP
     * itself (see class javadoc) — wire this to your voice transport.
     */
    protected void sendFrame(byte[] opusFrame) {
        // Intentionally a no-op placeholder seam; override or hook here.
    }

    private byte[] applyVolume(byte[] opusFrame) {
        // Volume for Opus-encoded frames can't be scaled post-encode without
        // decoding first; real volume control happens on the PCM side inside
        // FrameProvider in a full implementation. Kept as a pass-through seam
        // here since DSP-on-PCM is where filters (equalizer, timescale, etc.)
        // also need to hook in — see Filters.java for the parameter model.
        return opusFrame;
    }

    private void advanceQueue(String endReason) {
        AudioTrack finished = currentTrack.get();
        stopInternal(endReason);

        if (finished != null) {
            dispatcher.dispatchTrackEnd(sessionId, guildId, finished, endReason);
            scheduler.requeue(finished);
        }

        AudioTrack next = scheduler.poll();
        if (next != null) {
            startTrack(next);
        } else {
            playing.set(false);
        }
    }

    private void stopInternal(String reason) {
        if (sendLoopFuture != null) {
            sendLoopFuture.cancel(false);
            sendLoopFuture = null;
        }
        if (frameProvider != null) {
            frameProvider.stop();
            frameProvider = null;
        }
        if (ffmpeg != null) {
            ffmpeg.stop();
            ffmpeg = null;
        }
    }

    public synchronized void stop() {
        AudioTrack current = currentTrack.getAndSet(null);
        stopInternal("STOPPED");
        playing.set(false);
        if (current != null) {
            dispatcher.dispatchTrackEnd(sessionId, guildId, current, "STOPPED");
        }
        scheduler.clear();
    }

    public synchronized void skip() {
        advanceQueue("FINISHED");
    }

    public void setPaused(boolean value) {
        paused.set(value);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public void setVolume(int value) {
        this.volume = Math.max(0, Math.min(1000, value));
    }

    public int getVolume() {
        return volume;
    }

    public void setFilters(Filters filters) {
        this.filters = filters == null ? new Filters() : filters;
    }

    public Filters getFilters() {
        return filters;
    }

    public synchronized void seek(long positionMs) {
        AudioTrack track = currentTrack.get();
        if (track == null || !track.isSeekable()) {
            return;
        }
        try {
            stopInternal("SEEK");
            ffmpeg = new FFmpegBridge();
            InputStream pcm = ffmpeg.start(track.getPlaybackUri(), positionMs);
            OpusEncoder encoder = OpusEncoder.forQuality(opusQuality);
            frameProvider = new FrameProvider(encoder);
            frameProvider.setPositionMs(positionMs);
            frameProvider.start(pcm);
            playing.set(true);
            startSendLoop();
        } catch (Exception e) {
            log.error("[{}] Seek failed: {}", guildId, e.getMessage(), e);
        }
    }

    public AudioTrack getCurrentTrack() {
        return currentTrack.get();
    }

    public long getPositionMs() {
        return frameProvider != null ? frameProvider.getPositionMs() : 0;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public TrackScheduler getScheduler() {
        return scheduler;
    }

    public int getFrameDeficit() {
        return frameProvider != null ? frameProvider.getFramesDeficit() : 0;
    }

    private void emitPlayerUpdate() {
        dispatcher.dispatchPlayerUpdate(sessionId, guildId, getPositionMs(), playing.get());
    }

    public synchronized void destroy() {
        stopInternal("CLEANUP");
        if (updateLoopFuture != null) {
            updateLoopFuture.cancel(false);
        }
        AudioTrack current = currentTrack.getAndSet(null);
        if (current != null) {
            dispatcher.dispatchTrackEnd(sessionId, guildId, current, "CLEANUP");
        }
    }
}
