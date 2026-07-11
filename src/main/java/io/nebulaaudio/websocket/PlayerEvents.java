package io.nebulaaudio.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.nebulaaudio.source.AudioTrack;

/**
 * Event payload shapes sent over the player WebSocket, matching Lavalink v4's
 * op:"event" message family (TrackStartEvent, TrackEndEvent, etc.) plus the
 * periodic op:"playerUpdate" message.
 */
public final class PlayerEvents {
    private PlayerEvents() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static abstract class BaseEvent {
        public String op = "event";
        public String guildId;
    }

    public static class TrackStartEvent extends BaseEvent {
        public final String type = "TrackStartEvent";
        public AudioTrack track;

        public TrackStartEvent(String guildId, AudioTrack track) {
            this.guildId = guildId;
            this.track = track;
        }
    }

    public static class TrackEndEvent extends BaseEvent {
        public final String type = "TrackEndEvent";
        public AudioTrack track;
        public String reason; // FINISHED, LOAD_FAILED, STOPPED, REPLACED, CLEANUP

        public TrackEndEvent(String guildId, AudioTrack track, String reason) {
            this.guildId = guildId;
            this.track = track;
            this.reason = reason;
        }
    }

    public static class TrackExceptionEvent extends BaseEvent {
        public final String type = "TrackExceptionEvent";
        public AudioTrack track;
        public ExceptionInfo exception;

        public TrackExceptionEvent(String guildId, AudioTrack track, String message, String severity, String cause) {
            this.guildId = guildId;
            this.track = track;
            this.exception = new ExceptionInfo();
            this.exception.message = message;
            this.exception.severity = severity;
            this.exception.cause = cause;
        }

        public static class ExceptionInfo {
            public String message;
            public String severity;
            public String cause;
        }
    }

    public static class TrackStuckEvent extends BaseEvent {
        public final String type = "TrackStuckEvent";
        public AudioTrack track;
        public long thresholdMs;

        public TrackStuckEvent(String guildId, AudioTrack track, long thresholdMs) {
            this.guildId = guildId;
            this.track = track;
            this.thresholdMs = thresholdMs;
        }
    }

    public static class PlayerUpdateEvent {
        public final String op = "playerUpdate";
        public String guildId;
        public State state;

        public PlayerUpdateEvent(String guildId, long positionMs, boolean connected) {
            this.guildId = guildId;
            this.state = new State();
            this.state.time = System.currentTimeMillis();
            this.state.position = positionMs;
            this.state.connected = connected;
            this.state.ping = -1;
        }

        public static class State {
            public long time;
            public long position;
            public boolean connected;
            public long ping;
        }
    }
}
