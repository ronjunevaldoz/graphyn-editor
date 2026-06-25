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
| `media.file_output` | preview | implemented | no dedicated direct unit test yet | yes | yes | Pass-through file preview; only video-encode terminals expose a `file_path` |
| `preview.view` | preview | implemented | yes (`PreviewPluginTest`) | yes | yes | Generic opaque preview; used to surface TTS/mix audio handles |
| `graphyn.sticky_note` | sticky-notes | implemented | n/a (annotation) | yes | yes | No-op executor so an embedded guide note never fails execution |

## Template Coverage

| Template | Status | Covered by | Notes |
|---|---|---|---|
| Simple Text to Speech | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Path resolution, file read, and TTS wiring are verified. Output preview via `preview.view` (audio handle) |
| Video Narration | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Import, audio extraction, narration, mixing, encoding. Output preview via `media.file_output` |
| Audio Mix | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Video → extract + TTS → mix; caption-style metadata. Output preview via `preview.view` |
| Smart Video Encode | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Script-driven bitrate selection before encode. Output preview via `media.file_output` |
| Video Stitch | ready | `MediaWorkflowTemplateTest`, `MediaWorkflowExecutionTest` | Clip ordering, stitching, encode. Output preview via `media.file_output` |

Every template also carries a `graphyn.sticky_note` guide node (title, flow, use-cases, tips) and
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
- Phase 2 and Phase 3 nodes (captions, OCR, compositing, image ops) remain planned in
  `media-workflow-plan.md`.

## Known Bugs / Constraints

- **System dependencies required for real runs.** `media-core` shells out to FFmpeg/FFprobe and
  `media-ai` to the TTS binary. If these are absent on `PATH`, real execution fails at the node;
  the deterministic tests still pass because they fake the executors.
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
