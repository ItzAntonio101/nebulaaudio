package io.nebulaaudio.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;
import java.util.nio.charset.StandardCharsets;

/**
 * A resolved, playable track. `playbackUri` is what FFmpegBridge will actually
 * open (a direct media URL or local file path) — kept separate from the
 * public-facing `identifier` so sources can defer expensive resolution.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioTrack {
    private String identifier;
    private boolean seekable;
    private String author;
    private long length; // milliseconds
    private boolean stream;
    private String title;
    private String uri;
    private String sourceName;
    private String artworkUrl;
    private String isrc;

    // Not serialized to clients — internal only.
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String playbackUri;

    public String getEncoded() {
        // Lavalink encodes track info into an opaque base64 blob clients pass
        // back on /players. We do the same: encode a minimal pipe-delimited
        // representation. Not cryptographically signed — trusts the caller,
        // matching Lavalink's own (non-authenticated) track encoding model.
        String raw = String.join("\u0001",
                safe(sourceName), safe(identifier), safe(title), safe(author),
                String.valueOf(length), String.valueOf(stream), safe(uri), safe(playbackUri));
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static AudioTrack decode(String encoded) {
        String raw = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        String[] parts = raw.split("\u0001", -1);
        AudioTrack t = new AudioTrack();
        t.sourceName = parts[0];
        t.identifier = parts[1];
        t.title = parts[2];
        t.author = parts[3];
        t.length = Long.parseLong(parts[4]);
        t.stream = Boolean.parseBoolean(parts[5]);
        t.uri = parts[6];
        t.playbackUri = parts[7];
        t.seekable = !t.stream;
        return t;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @JsonProperty("info")
    public TrackInfo info() {
        TrackInfo info = new TrackInfo();
        info.identifier = identifier;
        info.isSeekable = seekable;
        info.author = author;
        info.length = length;
        info.isStream = stream;
        info.title = title;
        info.uri = uri;
        info.sourceName = sourceName;
        info.artworkUrl = artworkUrl;
        info.isrc = isrc;
        return info;
    }

    public static class TrackInfo {
        public String identifier;
        public boolean isSeekable;
        public String author;
        public long length;
        public boolean isStream;
        public String title;
        public String uri;
        public String sourceName;
        public String artworkUrl;
        public String isrc;
    }

    // --- getters/setters ---

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isSeekable() {
        return seekable;
    }

    public void setSeekable(boolean seekable) {
        this.seekable = seekable;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public String getIsrc() {
        return isrc;
    }

    public void setIsrc(String isrc) {
        this.isrc = isrc;
    }

    public String getPlaybackUri() {
        return playbackUri;
    }

    public void setPlaybackUri(String playbackUri) {
        this.playbackUri = playbackUri;
    }
}
