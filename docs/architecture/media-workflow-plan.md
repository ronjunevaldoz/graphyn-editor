# Media Workflow Plugin Suite

## Status

Phase 1 was implemented for JVM/Desktop on June 25, 2026. Current node status, unit test
coverage, and workflow execution coverage are tracked in
[Media Workflow Status](./media-workflow-status.md). Later phases remain planned and must not
expand the core workflow format until their platform backends are known.

## Review Findings

The original proposal had several blockers:

- It described eight Phase 1 nodes but defined seven, including a duplicate caption-style node.
- JVM-only FFmpeg code was placed in `commonMain`.
- Adding `VideoType`, `AudioType`, and `ImageType` to core would also require value,
  compatibility, serialization, documentation, and migration changes.
- `OpaqueValue` cannot carry a path or metadata.
- `media.video_encode` had no destination input.
- The proposed Qwen/Qute dependency is unpublished and unavailable to a reproducible build.
- The TTS cache key omitted `speed`, allowing incorrect cache hits.
- JNA bindings add ABI and packaging risk without improving the Phase 1 command-oriented work.
- Release tagging and publishing were mixed into feature implementation before integration
  tests existed.

## Refined Architecture

### Module Scope

Phase 1 adds two JVM-only modules:

1. `plugins/media-core` for FFmpeg-backed video and audio operations.
2. `plugins/media-ai` for provider-backed TTS and caption-style metadata.

They are installed by the Desktop host. Android, iOS, JS, and Wasm do not expose these nodes
until native or delegated backends exist.

### Media Handles

Media values are serializable `RecordValue` handles owned by `media-core`, not new core
primitives:

```text
VideoHandle = { kind: "video", path: String, mime_type: String }
AudioHandle = { kind: "audio", path: String, mime_type: String }
```

Their `RecordType` definitions use distinct single-value enum fields for `kind`, so connection
validation rejects audio-to-video wiring while workflows remain compatible with format version 1.

### FFmpeg Integration

The JVM backend invokes `ffmpeg` and `ffprobe` through `ProcessBuilder`.

- Executable paths can be overridden with `GRAPHYN_FFMPEG` and `GRAPHYN_FFPROBE`.
- Intermediate files live under `${GRAPHYN_HOME:-~/.graphyn}/temp`.
- Commands are passed as argument lists, never shell-concatenated strings.
- Phase 1 requires FFmpeg on `PATH`; no JNA or native bundle is added.

### TTS Integration

`media-ai` defines a `TextToSpeechEngine` boundary. The default JVM adapter invokes the
executable configured by `GRAPHYN_TTS_EXECUTABLE` with:

```text
--text <text> --language <language> --voice <voice> --speed <speed> --output <wav-path>
```

This keeps the Graphyn build reproducible while allowing the internal Qwen/Qute SDK to be
wrapped by a small CLI. Generated audio is cached under
`${GRAPHYN_HOME:-~/.graphyn}/cache/tts` using a SHA-256 key over text, language, voice, and
speed. Credentials remain the adapter's responsibility and are read from its environment.

## Phase 1 Nodes

### `plugins/media-core`

1. `media.video_import`
   - Inputs: `path`
   - Outputs: `video`, `width`, `height`, `duration_ms`, `fps`
   - Reads metadata with `ffprobe`; it does not decode frames.

2. `media.audio_extract`
   - Inputs: `video`
   - Outputs: `audio`, `sample_rate`, `duration_ms`
   - Extracts the first audio stream to PCM WAV.

3. `media.audio_mix`
   - Inputs: `audio_tracks`, optional `volumes`
   - Outputs: `audio`, `duration_ms`
   - Mixes tracks with FFmpeg `amix`. Empty `volumes` means `1.0` for every track.

4. `media.video_stitch`
   - Inputs: `videos`, `transition`
   - Outputs: `video`, `duration_ms`, `frame_count`
   - Phase 1 supports `transition = "cut"` using the concat demuxer. Fade is Phase 2 because
     it requires normalization and duration-aware filter graphs.

5. `media.video_encode`
   - Inputs: `video`, optional `audio`, `output_path`, `bitrate`, `codec`
   - Outputs: `file_path`, `size_bytes` (double), `duration_ms`
   - Phase 1 supports H.264 video and AAC audio in MP4.

### `plugins/media-ai`

6. `media.text_to_speech`
   - Inputs: `text`, `language`, `voice_id`, `speed`
   - Outputs: `audio`, `duration_ms`, `cached`

7. `media.caption_style`
   - Inputs: `color`, `background_color`, `font_size`, `position`
   - Output: `style_config`
   - Pure metadata node for the Phase 2 caption overlay.

All nodes use `FieldCardFactory` and register palette categories for video, audio, and media AI.

## Verification

- Spec and registration tests cover all seven nodes and their typed ports.
- Backend tests use injected fakes and do not require external services.
- FFmpeg integration tests generate tiny fixtures at runtime and skip when FFmpeg is absent.
- TTS tests use a fake engine and an isolated cache directory.
- Desktop bootstrap tests verify runtime and editor registration.
- `:plugins:media-core:test`, `:plugins:media-ai:test`, and `:app:desktopApp:compileKotlin`
  must pass.

## Deferred Work

### Phase 2

- Speech-to-text with word timestamps.
- Caption overlay and timing controller.
- Video composition.
- OCR.
- Fade transitions after stream normalization is defined.
- Server delegation for browser clients.

### Phase 3

- Image resize/crop.
- Audio resampling and encoding.
- Image-sequence rendering.
- Additional video codecs and containers.
- Android MediaCodec, iOS AVFoundation, and web/server backends.

## Release Gate

Version bump, Maven publication, release notes, and the `v0.4.0` tag happen only after:

1. CI passes on a machine with FFmpeg.
2. A configured TTS adapter completes a manual end-to-end workflow.
3. Licensing and distribution requirements for FFmpeg are documented.
