package io.nebulaaudio.source;

import java.util.List;

/**
 * Mirrors Lavalink v4's /v4/loadtracks response: a discriminated union over
 * loadType ("track", "playlist", "search", "empty", "error").
 */
public class LoadResult {
    public enum Type {
        TRACK, PLAYLIST, SEARCH, EMPTY, ERROR
    }

    public Type loadType;
    public Object data; // shape depends on loadType

    public static LoadResult track(AudioTrack track) {
        LoadResult r = new LoadResult();
        r.loadType = Type.TRACK;
        r.data = track;
        return r;
    }

    public static LoadResult search(List<AudioTrack> tracks) {
        LoadResult r = new LoadResult();
        r.loadType = Type.SEARCH;
        r.data = tracks;
        return r;
    }

    public static LoadResult playlist(String name, List<AudioTrack> tracks, Integer selectedTrack) {
        LoadResult r = new LoadResult();
        r.loadType = Type.PLAYLIST;
        PlaylistData pd = new PlaylistData();
        pd.info = new PlaylistInfo();
        pd.info.name = name;
        pd.info.selectedTrack = selectedTrack == null ? -1 : selectedTrack;
        pd.tracks = tracks;
        r.data = pd;
        return r;
    }

    public static LoadResult empty() {
        LoadResult r = new LoadResult();
        r.loadType = Type.EMPTY;
        r.data = new Object();
        return r;
    }

    public static LoadResult error(String message, String severity, String cause) {
        LoadResult r = new LoadResult();
        r.loadType = Type.ERROR;
        ErrorData ed = new ErrorData();
        ed.message = message;
        ed.severity = severity == null ? "common" : severity;
        ed.cause = cause == null ? "unknown" : cause;
        r.data = ed;
        return r;
    }

    public String getLoadType() {
        return loadType.name().toLowerCase();
    }

    public static class PlaylistInfo {
        public String name;
        public int selectedTrack;
    }

    public static class PlaylistData {
        public PlaylistInfo info;
        public List<AudioTrack> tracks;
    }

    public static class ErrorData {
        public String message;
        public String severity;
        public String cause;
    }
}
