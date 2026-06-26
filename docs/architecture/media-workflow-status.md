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
- `preview.view` (generic value preview, used to surface audio handles)
- `graphyn.sticky_note` (on-canvas guide annotation embedded in each template)

Phase 2 (captioning & composition) adds:

- `media.image_import` (loads an image handle + dimensions; produces the handle OCR consumes)
- `media.caption_overlay` (burns timed captions into a video)
- `media.video_compose` (layers overlay clips over a base video)
- `media.timing_controller` (averages sync points into delays)
- `media.speech_to_text` (audio → text + timed caption segments)
- `media.ocr` (image → text + bounding blocks)
- `media.video_overlay` + `media.overlays_list` (build the overlay list `video_compose` needs)
- `media.sync_point` + `media.sync_points_list` (build the sync-point list `timing_controller` needs)
- `media.audio_encode` (saves an audio handle to WAV/MP3/AAC so audio templates can output a file)

Phase 3 (image ops) adds:

- `media.image_resize` + `media.image_crop` (scale / trim an image)
- `media.images_list` + `media.image_sequence_to_video` (render images into an MP4 slideshow)

## Current Status

Phase 1 media workflows are implemented for JVM/Desktop and are covered by both template
contract tests and workflow execution tests.

Phase 2 nodes are implemented for JVM/Desktop and covered by plugin unit tests (with fakes) plus
availability-guarded FFmpeg backend tests. Every Phase 2 capability now ships in a template — four flows:
**Captioned Video** (`speech_to_text → caption_overlay` + `caption_style`), **Document Text Extract**
(`image_import → ocr`), **Picture-in-Picture** (`video_overlay → overlays_list → video_compose`),
and **Sync Calibration** (`sync_point → sync_points_list → timing_controller`). The builder +
collector nodes assemble the record lists `video_compose` and `timing_controller` consume.

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
| `media.audio_encode` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (TTS, Audio Mix) | yes | Saves audio to WAV/MP3/AAC; lets audio templates terminate in `media.file_output` |
| `media.text_to_speech` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes | yes | Cache key includes text, language, voice, and speed |
| `media.caption_style` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes | yes | Metadata-only node for Phase 2 caption overlays |
| `media.file_output` | preview | implemented | no dedicated direct unit test yet | yes | yes | Pass-through file preview; only video-encode terminals expose a `file_path` |
| `preview.view` | preview | implemented | yes (`PreviewPluginTest`) | yes | yes | Generic opaque preview; used to surface TTS/mix audio handles |
| `graphyn.sticky_note` | sticky-notes | implemented | n/a (annotation) | yes | yes | No-op executor so an embedded guide note never fails execution |
| `media.image_import` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Document Text Extract) | yes | FFprobe-backed image handle + dimensions; produces the image handle `media.ocr` consumes |
| `media.caption_overlay` | `media-core` | implemented | yes (`MediaCorePluginTest`, `FfmpegMediaCoreBackendTest`) | yes (Captioned Video) | yes | Burns captions via the `ass` filter; **requires FFmpeg built with libass** |
| `media.video_compose` | `media-core` | implemented | yes (`MediaCorePluginTest`, `FfmpegMediaCoreBackendTest`) | yes (Picture-in-Picture) | yes | Overlay-filter chain with per-overlay timing + opacity |
| `media.timing_controller` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Sync Calibration) | yes | Pure compute; averages `(source_ms,target_ms)` sync points into delays |
| `media.video_overlay` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Picture-in-Picture) | yes | Builds one overlay record for `video_compose` |
| `media.overlays_list` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Picture-in-Picture) | yes | Collects overlay records into the compose list |
| `media.sync_point` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Sync Calibration) | yes | Builds one `(source_ms,target_ms)` record |
| `media.sync_points_list` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Sync Calibration) | yes | Collects sync-point records into the timing list |
| `media.speech_to_text` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes (Captioned Video) | yes | CLI adapter `GRAPHYN_STT_EXECUTABLE`; emits caption segments |
| `media.ocr` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes (Document Text Extract) | yes | CLI adapter `GRAPHYN_OCR_EXECUTABLE`; pairs with `media.image_import` |
| `media.image_resize` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Image Edit) | yes | FFmpeg `scale`; outputs a resized image handle |
| `media.image_crop` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Image Edit) | yes | FFmpeg `crop`; trims to an x/y/w/h region |
| `media.images_list` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Slideshow) | yes | Collects image handles for the sequence encoder |
| `media.image_sequence_to_video` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Slideshow) | yes | Concat-demuxer slideshow at a fixed fps |

## Template Coverage

| Template | Status | Covered by | Notes |
|---|---|---|---|
| Simple Text to Speech | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Path resolution, file read, TTS → `audio_encode` (WAV) → `media.file_output` |
| Video Narration | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Import, audio extraction, narration, mixing, encoding. Output preview via `media.file_output` |
| Audio Mix | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Video → extract + TTS → mix → `audio_encode` (MP3) → `media.file_output`; caption-style metadata |
| Smart Video Encode | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Script-driven bitrate selection before encode. Output preview via `media.file_output` |
| Video Stitch | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Clip ordering, stitching, encode. Output preview via `media.file_output` |
| Captioned Video | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: transcribe → style → burn-in captions → encode. Output via `media.file_output` |
| Document Text Extract | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: import image → OCR → preview text. Needs `GRAPHYN_OCR_EXECUTABLE` to run |
| Picture-in-Picture | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: build overlay → compose over base → encode. Needs FFmpeg `overlay` filter |
| Sync Calibration | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: build sync points → average into delays → preview config. Pure compute |
| Image Edit | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 3: import → resize → crop → preview |
| Slideshow | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 3: import images → images_list → sequence → encode → output |

The launcher groups templates by `WorkflowCategory` (Media / Data & IO / Examples); media templates
are the Media section. Every template also carries a `graphyn.sticky_note` guide node (title, flow, use-cases, tips) and
ends in an output-preview node. Templates ship without positions; the editor runs auto-layout on
first load (see Editor Behavior).

## Editor Behavior

- **Auto-layout on load.** Demo templates have no stored node positions, so the editor dispatches
  `AutoLayout` once the canvas is measured (`GraphynApp`), guarded so a saved/edited layout is never
  clobbered. Triggered for all scenes, not just one.
- **Annotation parking.** `performAutoLayout` lays out the dataflow DAG, then parks annotation
  nodes (sticky guides) in a column to the left of the graph so they read as a legend. Annotation
  positions/sizes are included in the layout result so the viewport fit frames them too.
- **Default card.** Nodes without a registered `NodeCanvasFactory` (e.g. `io.resolve_path`) render
  through a default `FieldCardFactory` sized from their spec — there is no separate fallback card.
- **Output preview rule.** `media.file_output` requires a `file_path` (String); only
  `media.video_encode` terminals expose one. Audio/TTS terminals output an opaque audio handle, so
  those templates preview with `preview.view` instead.

## Verification Commands

Run the focused media checks:

```bash
./gradlew :app:app:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowTemplateTest
./gradlew :app:app:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowExecutionTest
./gradlew :plugins:media-core:test :plugins:media-ai:test
```

Run the full demo JVM suite:

```bash
./gradlew :app:app:jvmTest
```

## Known Gaps / Missing

- `media.file_output` still lacks a dedicated direct unit test file.
- The workflow-level execution tests use deterministic fakes (no FFmpeg/TTS launch). Real-tool
  coverage is per-node and availability-guarded: `FfmpegMediaCoreBackendTest` and the `say`/tesseract
  fallback tests. There is no single end-to-end test that runs a whole template against real tools.
- Speech-to-text has no zero-config fallback (no standard CLI), so the captioning template still
  needs `GRAPHYN_STT_EXECUTABLE`. TTS (macOS `say`) and OCR (`tesseract`) do fall back automatically.
- `media.video_stitch` supports only the `cut` transition in Phase 1.
- `media.video_compose` overlays are video handles only; image and text overlays are deferred.
- `media.image_import` reads only dimensions (no color space / frame extraction yet).
- Slideshow assumes input frames share dimensions (no auto-scale in `image_sequence_to_video`).
- Remaining Phase 3 nodes (`audio_resample`, advanced/custom encoding) are still planned in
  `media-workflow-plan.md`.

## Known Bugs / Constraints

- **System dependencies required for real runs.** `media-core` shells out to FFmpeg/FFprobe and
  `media-ai` to the TTS/STT/OCR binaries (`GRAPHYN_TTS_EXECUTABLE`, `GRAPHYN_STT_EXECUTABLE`,
  `GRAPHYN_OCR_EXECUTABLE`). If these are absent, real execution fails at the node; the deterministic
  tests still pass because they fake the executors/engines.
- **`media.caption_overlay` needs FFmpeg built with libass.** It burns subtitles via the `ass`
  filter. On a stripped FFmpeg the executor fails fast with a clear message
  (`FfmpegMediaCoreBackend.supportsFilter("ass")`), and the backend test skips that leg.
- **Working directory sensitivity.** `media.video_encode`'s `output_path` and resolved input paths
  are interpreted relative to the process working directory. The desktop app runs from
  `app/desktopApp/`, which is why templates resolve inputs via `io.resolve_path` against
  `../../app/app/src/commonMain/resources/media`. Generated outputs (`*.mp4`, `*.wav`) land in the
  desktop app dir and are git-ignored.
- **Script string escaping.** Kotlin Script (`script.eval`) code embedded in a triple-quoted
  workflow definition must escape template variables as `$$name` so the host compiler does not
  interpolate them (see `ScriptSpec` KDoc). JVM-only APIs such as `String.format` are fine — the
  script runs on the JVM.
- **`media.*` types are JVM-only.** The media plugins live in `src/main` (JVM). In the KMP
  `commonTest` registry they are unregistered, so scene validation ignores `unknown_node_type` for
  media scenes and lists media types in `jvmOnlyTypes`.

## Conflicts & Gotchas

- A node in a `WorkflowDefinition` always needs an executor or execution throws
  `"No executor registered for node type '…'"`. Annotation nodes are an editor concept the core
  engine does not know about, so `graphyn.sticky_note` registers a **no-op executor** to stay
  harmless inside an executable template.
- `media.file_output` and `preview.view` cards: the spec/executor are `commonMain`, but the JVM
  `media.file_output` card (Open button, file metadata) is `jvmMain`-only.
- Adding nodes to a template breaks the exact-match assertions in `MediaWorkflowTemplateTest` and
  the node-count assertions in `MediaWorkflowExecutionTest` — update both together.

## Do's and Don'ts

**Do**
- Resolve every file path through `io.resolve_path` (base dir + relative) instead of hardcoding.
- Terminate a video template in `media.video_encode → media.file_output`; terminate an audio/TTS
  template in `preview.view`.
- Give each template one `graphyn.sticky_note` guide (title, flow, use-cases, tips).
- Keep templates position-free and rely on auto-layout-on-load.
- Update `MediaWorkflowTemplateTest` and `MediaWorkflowExecutionTest` whenever a template changes.

**Don't**
- Don't wire an audio handle into `media.file_output` (it expects a `file_path` String).
- Don't hardcode absolute paths or assume the project root as the working directory.
- Don't add domain-specific nodes to the sample plugins (see `CLAUDE.md`).
- Don't bake node positions into a template — let auto-layout place them.
- Don't commit generated run outputs (`app/desktopApp/*.mp4` / `*.wav`); they are git-ignored.
