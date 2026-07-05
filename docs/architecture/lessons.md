# Engineering Lessons

Short index of durable lessons discovered while building Graphyn. Keep the canonical guidance in the code or in the more specific architecture/reference docs linked below.

## Workflow and AI

- Canonical settings keys should be snake_case, with legacy aliases only for compatibility.
- AI-generated workflows should emit `nodePositions`; auto-layout should stay a fallback.
- AI assistants are more useful when they can see node descriptions and layout state.
- Shorts prompt shaping belongs in a real node contract, not inline `script.eval` glue.
- Scene prompt scripts should fall back across caption/title/topic fields so one missing `prompt` key does not break the whole shorts pipeline.
- Reusable subgraphs work best when they expose one clean boundary value.
- `media.video_stitch` needs video clips, not stills; generate clips first, then stitch.
- Nullable workflow fields should round-trip as `NullValue`, not empty strings.
- Script-based media templates need the script plugin in the JVM runtime bundle.
- Desktop-only demo templates should be validated with the desktop runtime plugin set, or common-runtime tests will report missing-node false positives.
- Subgraph boundary ports still need explicit validation handling when they are intentionally injected.

## Stable Diffusion and Media Runtime

- Stable Diffusion workers should be selected by settings, not hardcoded hostnames.
- Keep the worker adapter behind one HTTP client and vary only URL plus API key per environment.
- Readiness, job polling, and cancelation checks should stay aligned with the worker OpenAPI contract.
- `script.eval`'s JSR-223 engine must be created fresh per call, not cached as a singleton — a
  shared engine's compiler state corrupts after a few different scripts, crashing later ones
  (even trivial ones) with an IR backend error unrelated to script complexity.
- Image-edit models (Qwen-Image-Edit, FLUX Kontext) condition on the source image as a **reference
  image** (`sd.id_cond.ref_images`), not `sd.img2img`'s denoising-strength `init_image` — confirmed
  against both models' own stable-diffusion.cpp reference commands, neither of which ever sets
  `--init-img`/`--strength`. Using img2img here silently ignores the source image's content
  entirely (at strength 1.0, 0.75, *and* 0.6 — strength wasn't the variable) and generates an
  unrelated image from the prompt alone. No error, just wrong output — validate any new
  image-conditioning wiring by eye, not just "did it run."
- Any client-side image/file path sent to server-sd must be staged (uploaded, then swapped to the
  server-returned path) before being included in the request JSON *and* in the pre-flight
  existence check — a local path slipping through either step fails silently (wrong output) rather
  than loudly (a clear "missing on server" error). `sd.id_cond.ref_images` was missed on both counts
  even after `init_image`/`control_image`/`mask_image` were handled correctly.
- A CLI/override mechanism that always serializes overrides as strings will silently fail for any
  non-string-typed port (a `strength=0.6` override landing on a `Double` field falls back to the
  port's default instead of applying) — convert to match the existing config value's type instead.
- Flux's `t5xxl` quant choice is a real VRAM budget line item, not a rounding error: swapping
  `Q5_K_M` (3.4GB) for `Q3_K_S` (2.1GB) took a Kontext edit from 30+ minutes (CPU-offloaded, <100MB
  VRAM free) to 63 seconds on the same 12GB card — the "encoder" is often the actual long pole once
  a diffusion checkpoint is quantized down already.
- `docker exec` over a remote TCP daemon connection can hang indefinitely (attached *and* detached)
  even when `docker ps`/`docker version` on the same connection return instantly — `exec` needs an
  HTTP connection-hijack the intervening network path may not relay correctly. Don't assume `exec`
  works just because other Docker API calls do.

### Known-good performance baselines

Reference points for "is this run just slow, or is something actually wrong" — on the 12GB RTX
5070 sd host, FLUX.1-schnell (Q4_K_S diffusion, `t5-v1_1-xxl-encoder-Q3_K_S`, 4 steps, 720×1280,
`cfgScale 1.0`, `distilledGuidance 3.5`, `flowShift 3.0`), a single `sd.txt2img` generation via
`/api/sd/generate-ex` with no other GPU consumer running:

- **~16-20s** is normal (confirmed repeatedly: 15.9s, 16.0s, 17.8s, 19.5s across separate runs).
- Isolated back-to-back calls on the same loaded context have shown a real but mild ~2x variance
  (44s → 88s) that's still GPU-bound the whole time (100% utilization, no CPU-offload signature).
- Anything in the hundreds of seconds (as seen in some `image-motion-storyboard-short` history
  entries: 400-560s) is the anomaly under investigation, not expected behavior — see the shorts
  pipeline scene-timing variance thread; root cause not yet confirmed (Ollama VRAM contention,
  fixed `sd-wrapper.cpp` compute-graph reuse, and idle VRAM margin were all tested and ruled out
  as the sole cause under controlled conditions).

### Known issues (open as of this writing)

- **Qwen-Image-Edit-2511 is disabled/skipped for now** (`DemoQwenImg2ImgDef.kt` carries a
  `TODO(qwen-edit-crash)`) — generation fails fast (~8s) with a `502 Bad Gateway`. Root-caused via
  `docker logs` on the sd host (`exec` hangs over the remote TCP connection, `logs` doesn't — see
  below): a native SIGSEGV inside stable-diffusion.cpp's `ggml_graph_cut` memory planner, while
  `LLM::LLMRunner::encode_image` (the Qwen2.5-VL vision encoder) processes the `sd.id_cond`
  reference image (`conditioner.hpp:1715 - QwenImageEditPlusPipeline`). Qwen-Image-Edit-2511's own
  components (diffusion + Qwen2.5-VL-7B-Instruct encoder + mmproj + LoRA) already exceed this
  12GB card's VRAM before any reference image is involved, forcing graph-cut to engage just to
  load — and that specific segment-measurement code path crashes. FLUX Kontext succeeds on the
  identical `sd.id_cond` code path because its full footprint fits without graph-cut engaging at
  all. This is vendored, read-only native code (`native/stable-diffusion.cpp`) — not fixable in
  Kotlin. Two remediation paths, neither pursued yet: (1) file upstream against
  `leejet/stable-diffusion.cpp` with the stack trace, or (2) retry with smaller Qwen
  diffusion/LLM quants so graph-cut isn't needed at all (same category as the Kontext t5xxl swap
  above).

## Catalog and Layout

- Launcher catalogs need explicit badge priority once recency and status both matter.
- Layout or zoom heuristics should preserve a consistent inset so graphs do not start on the border.

## Publishing and Gradle

- `implementation()` deps can still leak into the POM in KMP; published modules must be audited.
- The publishing audit should check both directions: listed modules apply the convention, and convention users are listed.
- `MavenPublishBaseExtension.coordinates()` is a setter, not a readable property.
- Retrying a partially published version on Maven Central can conflict with the existing deployment.

## State and Layout Bugs

- Never write from a fallback value that differs from the real initial state.
- Keep viewport and auto-layout limits aligned so manual zoom and layout behavior feel consistent.

## Extracting the shorts pipeline into a published plugin (step 0.2)

- The reusable storyboard/scene builders had to become a **multiplatform** module, not JVM-only:
  `WorkflowCatalog` is a `commonMain` enum whose entries (`ImageMotionShort`) call
  `imageMotionSceneSubgraph`/`stitchBatchSubgraph`, so those builders must resolve on every app
  target. Only `unloadOllamaModel()` is platform-specific — it's an `expect suspend fun` with real
  `java.net` actuals on jvm/android and no-op actuals on js/wasm/ios (those never drive Ollama).
- `graphyn-maven-publish`'s `verifyPublishing` auto-discovers published modules and asserts each
  appears in three files: `scripts/verify-maven-central.sh`, `.github/workflows/publish.yml`
  (matched by the `:path` minus leading colon), and `scripts/publish-local.sh`. There is no
  `publishedModulePaths` set in `build.gradle.kts` to edit despite the CLAUDE.md wording — the task
  enumerates `subprojects` with the vanniktech plugin applied.
- Kept the desktop `app/app/bootstrap` wiring thin via an app-side `ShortsBridge.kt` that re-aliases
  the moved public symbols under their old `internal` bootstrap names, so unrelated bootstrap files
  (video/image shorts, captioning) compiled unchanged. The app deliberately keeps its **own**
  `stitchBatchSubgraph` (with canvas `nodePositions`) in `DemoShortsScenes.kt`; the module's copy
  omits positions since positions are an editor-layout concern, not execution data.
- The `*RunTest` integration tests (`ImageMotionStoryboardShortRunTest`, `SdTemplateApiRunTest`,
  `MediaWorkflowExecutionTest`) execute against a live server-sd/Ollama/FFmpeg deployment and fail
  with "got null" output in any environment without it — not a regression signal. `sd.id_cond`
  unregistered in `DemoSceneWorkflowTest` is a separate pre-existing failure on `main`.
- The standalone `ollama.generate` node (Studio's `studio.generate-script` shape) is intentionally
  **additive** — it does the bare LLM call in one executor (`OllamaGenerate.kt`) and shares the
  storyboard subgraph's defaults (host `http://localhost:11434`, model `llama3.1`, `stream:false`,
  `keep_alive:0`) but does NOT reuse `storyboardGeneratorSubgraph`, which stays byte-for-byte
  unchanged so the verified `image-motion-storyboard-short` pipeline is untouched. The HTTP call is
  an injectable `transport: suspend (url, body) -> String` param on `ollamaGenerateExecutor(...)`,
  defaulting to the platform `expect suspend fun ollamaHttpPost` — this makes the response-parse and
  URL/body logic fully `commonTest`-able with a fake, no live server. js/wasm/ios actuals throw
  (not no-op like `unloadOllamaModel`) since the function must return a String; the executor catches
  it and degrades to `ok = false`. Needed `implementation(libs.serialization.json)` in shorts'
  commonMain (it was pulling JSON via `json.*` engine nodes before, so serialization wasn't a dep) —
  `implementation`, not `api`, so no POM leak / `verifyPublishing` failure.
