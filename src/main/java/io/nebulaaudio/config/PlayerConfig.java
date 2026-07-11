package io.nebulaaudio.config;

public class PlayerConfig {
    private int defaultVolume = 100;
    private int opusQuality = 10;

    public int getDefaultVolume() {
        return defaultVolume;
    }

    public void setDefaultVolume(int defaultVolume) {
        this.defaultVolume = defaultVolume;
    }

    public int getOpusQuality() {
        return opusQuality;
    }

    public void setOpusQuality(int opusQuality) {
        this.opusQuality = opusQuality;
    }
}
