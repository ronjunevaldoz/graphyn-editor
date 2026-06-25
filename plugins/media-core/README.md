# Graphyn Media Core

JVM/Desktop plugin providing FFmpeg-backed media workflow nodes.

## Requirements

- `ffmpeg` and `ffprobe` available on `PATH`
- Or set `GRAPHYN_FFMPEG` and `GRAPHYN_FFPROBE` to their executable paths
- Optional `GRAPHYN_HOME` override for intermediate files (default: `~/.graphyn`)

## Nodes

- `media.video_import`
- `media.audio_extract`
- `media.audio_mix`
- `media.video_stitch`
- `media.video_encode`

Media handles are typed records containing `kind`, `path`, and `mime_type`. Phase 1 stitching
supports compatible clips with a cut transition. Encode writes H.264/AAC MP4 to the required
`output_path` input.
