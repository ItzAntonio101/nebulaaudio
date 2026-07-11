package io.nebulaaudio.config;

public class SourceConfig {
    private boolean youtube = true;
    private boolean soundcloud = true;
    private boolean deezer = true;
    private boolean spotify = true;
    private boolean http = true;
    private boolean local = true;

    public boolean isYoutube() {
        return youtube;
    }

    public void setYoutube(boolean youtube) {
        this.youtube = youtube;
    }

    public boolean isSoundcloud() {
        return soundcloud;
    }

    public void setSoundcloud(boolean soundcloud) {
        this.soundcloud = soundcloud;
    }

    public boolean isDeezer() {
        return deezer;
    }

    public void setDeezer(boolean deezer) {
        this.deezer = deezer;
    }

    public boolean isSpotify() {
        return spotify;
    }

    public void setSpotify(boolean spotify) {
        this.spotify = spotify;
    }

    public boolean isHttp() {
        return http;
    }

    public void setHttp(boolean http) {
        this.http = http;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}
