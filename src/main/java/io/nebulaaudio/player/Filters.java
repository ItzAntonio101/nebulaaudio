package io.nebulaaudio.player;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Filter parameter set, matching Lavalink's /v4 filters payload shape.
 * This class holds *parameters* only. Actual DSP application happens in
 * AudioPlayer's frame pipeline (applyFilters), which reads these values
 * per-frame. Kept as a plain settable bag so PATCH /players can replace
 * individual filters independently, same as Lavalink's API contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Filters {
    private Float volume;
    private float[] equalizer; // 15 bands, gain -0.25..1.0 each
    private Karaoke karaoke;
    private Timescale timescale;
    private Tremolo tremolo;
    private Vibrato vibrato;
    private Rotation rotation;
    private Boolean lowPass; // simplified on/off + strength below
    private LowPass lowPassConfig;

    public static class Karaoke {
        public Float level = 1.0f;
        public Float monoLevel = 1.0f;
        public Float filterBand = 220.0f;
        public Float filterWidth = 100.0f;
    }

    public static class Timescale {
        public Float speed = 1.0f;
        public Float pitch = 1.0f;
        public Float rate = 1.0f;
    }

    public static class Tremolo {
        public Float frequency = 2.0f;
        public Float depth = 0.5f;
    }

    public static class Vibrato {
        public Float frequency = 2.0f;
        public Float depth = 0.5f;
    }

    public static class Rotation {
        public Double rotationHz = 0.0;
    }

    public static class LowPass {
        public Float smoothing = 20.0f;
    }

    /** Convenience preset: boosts bass bands of the 15-band equalizer. */
    public static Filters bassBoost() {
        Filters f = new Filters();
        f.equalizer = new float[]{0.6f, 0.7f, 0.8f, 0.55f, 0.25f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
        return f;
    }

    /** Convenience preset: nightcore = pitch + rate + speed up together. */
    public static Filters nightcore() {
        Filters f = new Filters();
        Timescale ts = new Timescale();
        ts.speed = 1.2f;
        ts.pitch = 1.2f;
        ts.rate = 1.0f;
        f.timescale = ts;
        return f;
    }

    public boolean isEmpty() {
        return volume == null && equalizer == null && karaoke == null && timescale == null
                && tremolo == null && vibrato == null && rotation == null && lowPassConfig == null;
    }

    // --- getters/setters ---

    public Float getVolume() {
        return volume;
    }

    public void setVolume(Float volume) {
        this.volume = volume;
    }

    public float[] getEqualizer() {
        return equalizer;
    }

    public void setEqualizer(float[] equalizer) {
        this.equalizer = equalizer;
    }

    public Karaoke getKaraoke() {
        return karaoke;
    }

    public void setKaraoke(Karaoke karaoke) {
        this.karaoke = karaoke;
    }

    public Timescale getTimescale() {
        return timescale;
    }

    public void setTimescale(Timescale timescale) {
        this.timescale = timescale;
    }

    public Tremolo getTremolo() {
        return tremolo;
    }

    public void setTremolo(Tremolo tremolo) {
        this.tremolo = tremolo;
    }

    public Vibrato getVibrato() {
        return vibrato;
    }

    public void setVibrato(Vibrato vibrato) {
        this.vibrato = vibrato;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public LowPass getLowPassConfig() {
        return lowPassConfig;
    }

    public void setLowPassConfig(LowPass lowPassConfig) {
        this.lowPassConfig = lowPassConfig;
    }
}
