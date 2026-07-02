# Graphyn Media Core

JVM/Desktop plugin providing FFmpeg-backed media workflow nodes.

## Requirements

- `ffmpeg` and `ffprobe` available on `PATH`
- Or set `GRAPHYN_FFMPEG` and `GRAPHYN_FFPROBE` to their executable paths
- Optional `GRAPHYN_HOME` override for intermediate files (default: `~/.graphyn`)
- `media.caption_overlay` additionally requires an FFmpeg built with **libass** (the `ass` filter)

## Nodes

Phase 1 — decode / encode:

- `media.video_import`
- `media.audio_extract`
- `media.audio_mix`
- `media.video_stitch`
- `media.video_encode`

Phase 2 — captioning / composition:

- `media.image_import` — loads an image handle and reads its pixel dimensions (producer for OCR)
- `media.caption_overlay` — burns timed captions onto a video using a caption style
- `media.video_compose` — layers overlay clips over a base video with per-overlay timing + opacity
- `media.timing_controller` — averages measured `(source_ms, target_ms)` sync points into delays

Media handles are typed records containing `kind`, `path`, and `mime_type`. Phase 1 stitching
supports compatible clips with a cut transition. Encode writes H.264/AAC MP4 to the required
`output_path` input.


## Recommended structure (can be discuss)
ffmpeg/
├── FfmpegMediaCoreBackend.kt
├── FfmpegVideoExtensions.kt
├── FfmpegImageExtensions.kt
├── FfmpegAudioExtensions.kt
├── FfmpegCaptionExtensions.kt   ⭐
├── FfmpegComposeExtensions.kt   ⭐
├── renderer/
│   ├── CaptionRenderer.kt
│   ├── AssCaptionRenderer.kt
│   ├── DrawTextCaptionRenderer.kt
│   └── WebVttCaptionRenderer.kt
└── util/
├── FfmpegFilterUtils.kt
├── AssUtils.kt
└── TimeUtils.kt