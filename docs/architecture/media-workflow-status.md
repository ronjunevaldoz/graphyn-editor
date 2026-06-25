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

## Current Status

Phase 1 media workflows are implemented for JVM/Desktop and are covered by both template
contract tests and workflow execution tests.

Phase 2 nodes are implemented for JVM/Desktop and covered by plugin unit tests (with fakes) plus
availability-guarded FFmpeg backend tests. The **Captioned Video** demo wires the captioning chain
(`speech_to_text → caption_overlay`, plus `caption_style`) end-to-end. `video_compose`,
`timing_controller`, `image_import`, and `ocr` are registered in the palette but not yet used in a
shipped demo.

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
| `media.file_output` | preview | implemented | no dedicated direct unit test yet | yes | yes | Pass-through file preview; only video-encode terminals expose a `file_path` |
| `preview.view` | preview | implemented | yes (`PreviewPluginTest`) | yes | yes | Generic opaque preview; used to surface TTS/mix audio handles |
| `graphyn.sticky_note` | sticky-notes | implemented | n/a (annotation) | yes | yes | No-op executor so an embedded guide note never fails execution |
| `media.image_import` | `media-core` | implemented | yes (`MediaCorePluginTest`) | yes (Document Text Extract) | yes | FFprobe-backed image handle + dimensions; the producer `media.ocr` consumes |
| `media.caption_overlay` | `media-core` | implemented | yes (`MediaCorePluginTest`, `FfmpegMediaCoreBackendTest`) | yes (Captioned Video) | yes | Burns captions via the `ass` filter; **requires FFmpeg built with libass** |
| `media.video_compose` | `media-core` | implemented | yes (`MediaCorePluginTest`, `FfmpegMediaCoreBackendTest`) | no | n/a | Overlay-filter chain with per-overlay timing + opacity |
| `media.timing_controller` | `media-core` | implemented | yes (`MediaCorePluginTest`) | no | n/a | Pure compute; averages `(source_ms,target_ms)` sync points into delays |
| `media.speech_to_text` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes (Captioned Video) | yes | CLI adapter `GRAPHYN_STT_EXECUTABLE`; emits caption segments |
| `media.ocr` | `media-ai` | implemented | yes (`MediaAiPluginTest`) | yes (Document Text Extract) | yes | CLI adapter `GRAPHYN_OCR_EXECUTABLE`; pairs with `media.image_import` |

## Template Coverage

| Template | Status | Covered by | Notes |
|---|---|---|---|
| Simple Text to Speech | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Path resolution, file read, and TTS wiring are verified. Output preview via `preview.view` (audio handle) |
| Video Narration | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Import, audio extraction, narration, mixing, encoding. Output preview via `media.file_output` |
| Audio Mix | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Video → extract + TTS → mix; caption-style metadata. Output preview via `preview.view` |
| Smart Video Encode | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Script-driven bitrate selection before encode. Output preview via `media.file_output` |
| Video Stitch | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Clip ordering, stitching, encode. Output preview via `media.file_output` |
| Captioned Video | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: transcribe → style → burn-in captions → encode. Output via `media.file_output` |
| Document Text Extract | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Phase 2: import image → OCR → preview text. Needs `GRAPHYN_OCR_EXECUTABLE` to run |

The launcher groups templates by `WorkflowCategory` (Media / Data & IO / Examples); media templates
are the Media section. Every template also carries a `graphyn.sticky_note` guide node (title, flow, use-cases, tips) and
ends in an output-preview node. Templates ship without positions; the editor runs auto-layout on
first load (see Editor Behavior).

## Editor Behavior

- **Auto-layout on load.** Demo templates have no stored node positions, so the editor dispatches
  `AutoLayout` once the canvas is measured (`DemoApp`), guarded so a saved/edited layout is never
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
./gradlew :app:demo:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowTemplateTest
./gradlew :app:demo:jvmTest --tests com.ronjunevaldoz.graphyn.bootstrap.MediaWorkflowExecutionTest
./gradlew :plugins:media-core:test :plugins:media-ai:test
```

Run the full demo JVM suite:

```bash
./gradlew :app:demo:jvmTest
```

## Known Gaps / Missing

- `media.file_output` still lacks a dedicated direct unit test file.
- The workflow execution tests use deterministic fakes, so they validate wiring and data flow
  **without** launching FFmpeg or the TTS binary. There is no end-to-end test that produces a real
  media file.
- No audio encode/save node yet, so audio-only templates cannot terminate in `media.file_output`
  (they use `preview.view`).
- `media.video_stitch` supports only the `cut` transition in Phase 1.
- **`media.video_compose` and `media.timing_controller` are not yet in a demo template.** Both
  consume *lists of records* (overlays / sync points) that have **no in-graph producer node**, so a
  clean demo needs an overlay/sync-point builder node first (Phase 3).
- `media.video_compose` overlays are video handles only; image and text overlays are deferred.
- `media.image_import` reads only dimensions (no color space / frame extraction yet).
- Phase 3 nodes (image ops, audio encode, advanced encoding) remain planned in
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
  `../../app/demo/src/commonMain/resources/media`. Generated outputs (`*.mp4`, `*.wav`) land in the
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
