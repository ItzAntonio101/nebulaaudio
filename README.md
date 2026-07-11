# NebulaAudio

A Lavalink-style Discord audio node — REST + WebSocket API, pluggable audio
sources, FFmpeg/Opus pipeline, filters, and a JAR-based plugin system.

## Status — read before deploying

This is an MVP skeleton. Honest breakdown:

**Fully implemented, should work as-is:**
- Config loading (`nebula.yml` via SnakeYAML)
- REST API: `/v4/loadtracks`, `/v4/info`, `/v4/stats`, player PATCH/DELETE/GET
- WebSocket: session handling, `ready`/`playerUpdate`/`TrackStart`/`TrackEnd`/
  `TrackException`/`TrackStuck` events
- HTTP and local-file (`file:`) audio sources — genuinely playable end to end
- FFmpeg → PCM → Opus pipeline (via FFmpegBridge → FrameProvider → OpusEncoder)
- Track scheduling/queue, loop modes, plugin loader (ServiceLoader-based)

**Stubbed, not implemented (return a clear `not_implemented` error, not a fake success):**
- YouTube, SoundCloud, Deezer, Spotify sources — each file explains *why*
  (extraction complexity, ToS/DRM constraints) and sketches the real
  extension path. See the javadoc at the top of each class in `source/`.
- Discord voice UDP/RTP transmission — `AudioPlayer.sendFrame()` is the seam
  where Opus packets are produced; actually sending them over a Discord voice
  socket is a separate subsystem this build doesn't include.
- Cluster mode — config is loaded and logged, but node discovery/failover/
  player migration networking is not implemented.
- Filter DSP — `Filters` holds all the parameters the API accepts (equalizer,
  bass boost, nightcore, timescale, karaoke, tremolo, vibrato, rotation,
  lowpass) and they round-trip correctly through PATCH, but they are not yet
  applied to the PCM stream. `AudioPlayer.applyVolume()` is the hook point.


## Build & run

```
./gradlew build
java -jar build/libs/NebulaAudio.jar
```

Needs `ffmpeg` on PATH (or set `FFMPEG_PATH` env var) for actual playback.

## Layout

Matches the requested structure, with one addition: `plugin/` (holding
`NebulaPlugin` + `PluginLoader`) since the spec required the interface but
didn't list a package for it.
