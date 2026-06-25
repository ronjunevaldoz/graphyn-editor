# Media Workflow Status

Canonical status and verification snapshot for the media workflow suite.

This page covers the demo workflows that ship with Graphyn and the node types they depend on:

- `io.resolve_path`
- `io.file_read`
- `media.video_import`
- `media.audio_extract`
- `media.audios_list`
- `media.audio_mix`
- `media.videos_list`
- `media.video_stitch`
- `media.video_encode`
- `media.text_to_speech`
- `media.caption_style`
- `media.file_output`

## Current Status

Phase 1 media workflows are implemented for JVM/Desktop and are covered by both template
contract tests and workflow execution tests.

## Node Status

| Node | Area | Implementation | Direct unit test | Template wiring test | Workflow execution test | Notes |
|---|---|---|---|---|---|---|
| `io.resolve_path` | shared IO helper | implemented | yes (`IoPluginTest`) | yes | yes | Resolves demo resource paths before file reads |
| `io.file_read` | shared IO helper | implemented | yes (`IoPluginTest`) | yes | yes | Reads the text fixtures used by the demo templates |
| `media.video_import` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | FFprobe-backed video metadata import |
| `media.audio_extract` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | Extracts the first audio stream to WAV |
| `media.audios_list` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | Collector helper used before `media.audio_mix` |
| `media.audio_mix` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | FFmpeg-backed mix with optional volumes |
| `media.videos_list` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | Collector helper used before `media.video_stitch` |
| `media.video_stitch` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | Cut-only concat flow in Phase 1 |
| `media.video_encode` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes | yes | MP4 render with destination `output_path` input |
| `media.text_to_speech` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes | yes | Cache key includes text, language, voice, and speed |
| `media.caption_style` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes | yes | Metadata-only node for Phase 2 caption overlays |
| `media.file_output` | preview | implemented | no dedicated direct unit test yet | yes | yes | Pass-through preview node for the encoded media file |

## Template Coverage

| Template | Status | Covered by | Notes |
|---|---|---|---|
| Simple Text to Speech | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Path resolution, file read, and TTS wiring are verified |
| Video Narration | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Covers import, audio extraction, narration, mixing, and encoding |
| Audio Mix | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Covers audio collection, mix, and caption styling metadata |
| Smart Video Encode | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Covers script-driven bitrate selection before encode |
| Video Stitch | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Covers clip ordering, stitching, encode, and preview output |

## Verification Commands

Run the focused media checks:

```bash
./gradlew :app:demo:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowTemplateTest
./gradlew :app:demo:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowExecutionTest
```

Run the full demo JVM suite:

```bash
./gradlew :app:demo:jvmTest
```

## Known Gaps

- `media.file_output` still lacks a dedicated direct unit test file.
- The workflow execution tests use deterministic fakes, so they validate wiring and data flow
  without launching FFmpeg or the TTS binary.
- Phase 2 and Phase 3 nodes remain planned in `media-workflow-plan.md`.
