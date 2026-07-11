# Engineering Lessons

Short index of durable lessons discovered while building Graphyn. Keep the canonical guidance in the code or in the more specific architecture/reference docs linked below.

## Workflow and AI

- Canonical settings keys should be snake_case, with legacy aliases only for compatibility.
- AI-generated workflows should emit `nodePositions`; auto-layout should stay a fallback.
- AI assistants are more useful when they can see node descriptions and layout state.
- Shorts prompt shaping belongs in a real node contract, not inline `script.eval` glue.
- Scene prompt scripts should fall back across caption/title/topic fields so one missing `prompt` key does not break the whole shorts pipeline.
- Comparison shorts had a hidden minimum length from the per-pair duration floor; if the result feels too long, check the floor and pair count before assuming the narration is the only driver.
- Reusable subgraphs work best when they expose one clean boundary value.
- A host app can silently shadow a shared canvas renderer with a local `registerCanvasCard` override; if a new card is not showing up, check the app/plugin registry before assuming the shared factory is wrong.
- Editor connection creation should enforce single incoming connections by default and only allow fan-in on list-typed inputs; otherwise the graph becomes valid only after a validator pass.
- Intra-node progress (e.g. diffusion steps) is reported via a `ProgressReporter` `CoroutineContext.Element` the engine installs around each `executor.execute()` — executors call the opt-in `suspend reportProgress(step, total, phase)`; this keeps `NodeExecutor`'s single-method `fun interface` SAM signature intact so the dozens of `NodeExecutor { }` lambda call sites are untouched. Reports surface as `ExecutionEvent.Progress` on the same `onEvent`/`ExecutionStreamMessage` stream as `Started`/`Succeeded`, so SSE (`GET /executions/{id}/events`) carries them with zero server change. Adding the sealed case only breaks *exhaustive* `when(event)` blocks (one existed: `GraphynEditorExecutionActions`); `.any {}`-style predicate tests are source-compatible.
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
- **2026-07-07 update:** re-ran `workflow=regenerate-scene` (one Flux scene regen + stitch +
  caption + TTS + encode against the same host) after a round of server-sd native fixes made the
  same day — `te=cpu` text-encoder-offload was dead code before this and is now actually wired,
  and a VAE-tiling override bug (an env-only block that could silently force tiling with a
  hardcoded `rel_size` regardless of Kotlin config) was fixed. Result: **89s total** for the whole
  16-node run, not 400-560s — the anomaly did not reproduce. Not a confirmed root-cause fix (only
  one re-test, and the fixes targeted different specific bugs, mostly on the Wan video path), but
  a strong signal worth noting before assuming this is still an open problem.
- **2026-07-07 Modal L4 480p baseline:** full 3-scene `workflow=storyboard` (Ollama + 3 Flux
  calls at 480x848 + Ken Burns + stitch + caption + TTS + encode, 25 nodes) against the Modal
  `serve_image` (L4) deployment instead of the Windows host: **99s total, zero errors.** Fixed a
  real blocker along the way — `ImageMotionScene.kt`'s hardcoded `FLUX_T5XXL` path
  (`t5-v1_1-xxl-encoder-Q3_K_S.gguf`) doesn't exist on the Modal volume or in server-sd's model
  catalog; switched to `t5xxl_Q5_K_M.gguf`, which does. Output verified visually (real food
  photography, correctly-timed burned-in captions, correct 480x848 dimensions). `width`/`height`
  are now optional params on `imageMotionSceneSubgraph(Dynamic)`/`imageMotionStoryboardShortWorkflow`
  (default to `ShortsConstants.WIDTH/HEIGHT`) instead of being hardcoded, and the CLI runner
  accepts `width=`/`height=` args, so a Modal/low-res test run no longer requires touching the
  shared constants other consumers rely on.
- **2026-07-07 character-sheet reference-image consistency, first real test:** added
  `characterSheetSubgraphDynamic` (`plugins/shorts/.../CharacterSheetScene.kt`) — one Flux
  portrait of the storyboard's `character` field, generated once before the scene loop — and a new
  `useCharacterSheet` param on `imageMotionSceneSubgraphDynamic` that swaps each scene's diffusion
  checkpoint to FLUX Kontext, adds an `sd.id_cond` node wired to the character sheet's image
  (auto-wrapped into a one-element list by `buildInputMap`'s existing list-port collection logic —
  no wrapper node needed, verified by reading `WorkflowExecutionScheduling.kt` directly), and bumps
  sampling from 4 to 20 steps (matching `DemoFluxKontextImg2ImgDef.kt`'s validated config). Full
  pipeline with this enabled, against Modal L4 at 480x848: **183s total, all 26 nodes succeeded.**
  Visually verified across all 3 scene clips: same dark hair, same face shape, same skin tone, same
  general styling in every scene — a real, positive result, not just "no errors." This was
  genuinely untested before (Kontext's only prior validated use was editing one specific photo, not
  conditioning multiple different scene compositions off a reusable reference) — it works
  noticeably better than the text-only baseline. Off by default (`useCharacterSheet = false`) since
  it costs meaningfully more time per scene; opt in via `character_sheet=true` on the CLI runner.
- **2026-07-07 multi-reference (3 poses/expressions) + full 720x1280 resolution:** generalized
  `characterSheetSubgraphDynamic` to take a `poseInstruction`/`expressionDetail` (see
  `CharacterSheetPoses`: NEUTRAL/SMILING/ACTION) and call it 3x as separate outer nodes, all wired
  into every scene's `ref_images` list port (multiple connections to one list-typed port collect
  automatically — confirmed no wrapper/adapter node needed). A single grid image with multiple
  poses was considered and rejected: Kontext conditions on one reference as a whole and can't be
  told "use only this panel," so a composite would likely confuse it rather than help — separate
  images into the same list port is the supported shape. Full pipeline, 3 references, full
  720x1280 (not the 480p test tier) against Modal L4: **398s total, all 28 nodes succeeded.**
  Visual result was, if anything, more convincing than the single-reference test — same
  hair-in-a-bun styling, same face shape, same warm cinematic look recognizable across all 3
  scenes. Still comfortably a single test iteration (under 7 minutes), not the original 20+.

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

- **No engine-level concurrency-limit primitive for GPU-bound node types.** Every shorts workflow
  that runs multiple `sd.*` generations against the same GPU-bound server (comparison-short's
  mascot + 8 photo generations, storyboard's N scenes, etc.) serializes them by manually wiring
  one node's data *output* into an unrelated downstream node's `gate` *input* purely so the
  scheduler treats it as a dependency — the `gate` port doesn't use the value, only the fact that
  the connection exists. This works, but it means every workflow author has to hand-thread these
  fake edges through an otherwise-unrelated part of the graph, and it visibly clutters the graph
  (confirmed: real user feedback on the comparison-short workflow's rendered graph called this out
  specifically, independent of auto-layout quality). Compare to ComfyUI, which solves the identical
  single-GPU-serialization problem for free via its own sequential execution model — no manual
  gate-wiring needed at all. The clean fix is a first-class primitive in `WorkflowExecutionEngine`
  (e.g. a node-type or resource-tag-based concurrency cap declared once, not threaded through every
  workflow's own connections) — that's a `core/execution` change, not something fixable by
  rearranging Kotlin workflow-builder code in this repo. Not pursued yet; flagging so it doesn't
  get silently re-solved with more gate-wiring in the next workflow that needs GPU serialization.

## Catalog and Layout

- Launcher catalogs need explicit badge priority once recency and status both matter.
- Layout or zoom heuristics should preserve a consistent inset so graphs do not start on the border.
- Collapsed subgraph nodes should use a dedicated boundary card/factory instead of borrowing the generic FieldCard; otherwise the UI reads like a fake port list, and any extra section labels must be reflected in the port-anchor math so the canvas dots stay aligned.

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
- Converted the SD server URL/API key from an app-wide `GraphynSettings` value into a graph-level
  `sd.server` config node (multi-server support, e.g. Modal deployments) without touching the
  `StableDiffusionBackend` interface: the override rides along as a plain nullable field
  (`SdServerConfig`) on `SdGenerateImageRequest`/`SdGenerateVideoRequest`, and `ServerSdClient`
  merges it over the settings-resolved `SdConnection` only for that one call. Every other
  `ServerSdClient` method (`ping`/`status`/`jobs`/`cancel`/`unload`/`load`) keeps its old app-wide
  behavior via a nullable `conn: SdConnection? = null` param resolved lazily inside the function
  body — Kotlin rejects a suspend call as a default-parameter *value* (`conn: SdConnection =
  connection()` fails to compile with "Suspend function call in default parameter value is
  unsupported"), so the resolution has to happen in the body, not the signature. Also had to thread
  the override into `ensureReady()`/`ready()` explicitly; they call `ping`/`status`/`jobs`
  internally, so without passing the resolved `conn` through, readiness would silently check the
  wrong server while generation hit the right one.
- Auto-layout "nodes pile up on the canvas edge" had three compounding causes, none in the layering
  itself: `GraphynNodeLayoutState.clamp()` clamped every node with a fixed 280×180
  `GraphynCanvasMetrics.NodeSize` (so all overflowing nodes collapsed onto the *same* max-edge
  coordinate), `GraphynAutoLayout` centered its bounding box on the 8192×6144 logical canvas without
  checking the box actually fit, and annotation placement (`graphMinX - maxAnnW - gap`) went
  negative and got clamp-collapsed to 0. Rule: any coordinate producer that feeds
  `setNodePosition` must stay within canvas bounds itself — the clamp is a last resort that
  destroys relative layout, not a fitting mechanism. Fix pattern: per-node size resolution lives in
  one shared `GraphynEditorState.resolveNodeSize(type)` extension (registry → spec-derived
  FieldCard → default), injected into `GraphynNodeLayoutState` as a `(String) -> IntSize` lambda.
- Tree-band vertical placement (bandH = sum of children, first parent claims child's bandStart)
  double-books space for diamond dependencies — a shared child belongs to *every* parent's band.
  Replaced with per-column barycenter placement: y = average of placed parents' centers, floored by
  a per-column `nextFreeY` cursor. Simpler (fewer lines) and overlap-free by construction.
- Writing a test that shows BFS layout *crossing* edges is subtle: with single-parent children, BFS
  visit order naturally mirrors parent order, so edges don't cross. A crossing needs a multi-parent
  child whose enqueue is delayed until its last parent is visited (e.g. edges `b→d, a→c, b→c`: `c`
  waits for both `a` and `b`, so `d` enqueues first and `a→c` must cross `b→d`).
- Kotlin gotcha: adding a new parameter *after* a trailing-lambda parameter silently breaks every
  `f(x, y) { ... }` call site (the lambda binds to the new last param). Keep the lambda param last
  when extending a signature.
- **2026-07-07 regenerate-scene edit mode fails on Modal, works on Windows — Modal-specific,
  not a code bug:** `workflow=regenerate-scene index=N edit=true instruction='...'` (the new
  `imageMotionSceneEditSubgraph` Kontext+`sd.id_cond` path) reproducibly failed against Modal
  L4 with `server-sd responded 303:` (empty body) on the actual `/api/sd/generate-ex` call,
  after running the full ~160s generation — twice, including an immediate retry on the likely
  same warm container (`max_containers=1`). Ruled out: a bug in the new subgraph (compared
  line-by-line against the proven `useCharacterSheet` Kontext path, no meaningful difference);
  server-sd's own Ktor routes (grepped for redirect logic — only a 302 for video/Cloudinary,
  unrelated); Modal function timeout config (`timeout=600`, `startup_timeout=300`, both
  generous for a 160s call). `modal app logs graphyn-sd` didn't surface a clear HTTP
  request/response line to pin down further. **Re-ran the identical command against the
  Windows host (`default` environment) — succeeded cleanly: 258s, all 16 nodes, valid
  720x1280 h264 + aac output.** This isolates the failure to something Modal-specific (its
  `web_server` proxy layer, most likely) rather than the new edit-mode code. Root cause still
  unresolved — using Windows for regenerate-scene edit mode until it is. Also surfaced:
  `regenerate-scene` doesn't thread `width=`/`height=` through at all (unlike `storyboard`),
  so an edited scene can come out at a different resolution than the rest of the short if the
  original run used non-default dimensions — didn't cause a hard failure in this test, but is
  a real gap worth fixing.
- Subgraph connection endpoints were misaligned from their port dots because
  `GraphynConnectionLayer`/`GraphynConnectionMidpoints` resolved specs via `nodeSpecs.resolve(type)`
  only — null for editor-created subgraph nodes (no registered spec) — falling back to the generic
  `GraphynCanvasMetrics.portAnchorY` *and* coercing every port index to 0, while the dots in
  `GraphynNodeLayer` used the boundary-derived spec + `SubgraphNodeCardFactory`. Rule: every
  consumer of port geometry must go through one shared resolver
  (`resolveRenderSpec`/`resolvePortAnchor` in `GraphynPortAnchorResolver.kt`) — duplicated
  spec-resolution chains *will* drift.
- `EnumType` and `MultiEnumType` silently shared a port color, and color is load-bearing
  (`PortCompatibility` uses `portColor` as a semantic channel id for opaque ports).
  `GraphynPortTypeColorTest` now asserts one-color-per-type; extend its `baseTypes` list when
  adding a `WorkflowType` variant or the test won't guard it.
- `deriveSubgraphSpec` now filters boundary inputs to `required` ones — display contract only;
  `subgraphBoundary` (execution injection, expand rewiring) stays unfiltered. Known limitation:
  an outer connection into an *optional* boundary input (possible when collapsing a selection
  whose crossing edge fed an optional port) references a hidden port and anchors at row 0.
- Collapsed subgraph nodes had a second, bespoke card (`SubgraphNodeCard`) with its own
  HEADER/ROW/DIVIDER/WIDTH constants, separate from `FieldCardFactory`'s. Both computed
  `portAnchorY` correctly for their own geometry, so it wasn't a bug, just permanent drift risk —
  and it meant subgraph nodes looked structurally different (width, header height) from every
  other card. Deleted it; subgraph nodes now render via the same `FieldCardFactory` as any other
  node, sized from the derived boundary spec. The one thing the bespoke card had that `FieldCard`
  didn't — double-click-to-enter — became a **generic** `FieldCard` behavior gated on
  `ctx.onEnterSubgraph != null` (already null for every non-subgraph node), so it needed no
  subgraph-specific branching in a general-purpose, published card (`graphyn-ui-cards`).
- `deriveSubgraphSpec`'s required-only filter (previous entry) had a gap: an outer connection into
  an *optional* boundary input becomes invisible on the card, so the wire has nothing to land on.
  Fixed by adding `connectedInputs: Set<String>` — a port is shown when `required || connected`.
  Threading this through required passing the *outer* workflow (not just the node) into every
  consumer of `deriveSubgraphSpec`/`resolveNodeFactory`/`resolvePortAnchor`
  (`GraphynNodeLayer`, `GraphynConnectionLayer`, `GraphynConnectionMidpoints`,
  `GraphynCanvasGestures`, `GraphynMinimapDebugger`, `GraphynEditorViewportActions`,
  `GraphynInspectorPanel`) — a reminder that a "just add a parameter" fix to a shared resolver
  fans out to every call site, not just the one you're looking at.
- The first pass at the "↳ Enter" subgraph hint used an `Alignment.BottomEnd`-aligned `Box`
  overlaid on the card — it painted directly on top of the last port row's text whenever the card
  had no output rows to absorb it (screenshot caught it immediately). Floating UI over
  content-that-can-vary-in-height is a trap; the fix reserves real space instead: `FieldCardFactory`
  gained a `hasEnterHint: Boolean` constructor param that adds `ENTER_HINT_DP` to `nodeHeight`, and
  the hint renders as a genuine trailing `Column` child, not an overlay. Port anchors are
  unaffected since the row is appended *after* the last output — `portAnchorY` never reads
  `nodeHeight`. Rule: always render a Roborazzi screenshot for a new visual element before calling
  it done — an overlap like this is invisible in a text-only diff review.
- Qwen3 TTS voice drift showed up when narration used a blank `voice` and let the model fall back
  to its default expressive speaker. The fix was to pin a named Qwen3 CustomVoice speaker
  (`Ryan`) for the default narration path and keep OuteTTS as the explicit alternate engine.
  Rule: if narration must stay consistent across many clips, do not rely on the model's implicit
  default voice; pick a speaker or clone a reference voice on purpose.
- For result metadata on comparison shorts, use `kotlin.time.Clock.System.now()` in a tiny compiled
  node instead of a scripted timestamp or a new datetime dependency. The first pass reached for the
  wrong `kotlinx.datetime.Clock` API, which does not expose `System` in this codebase's dependency
  mix. Rule: match the repo's existing clock API before adding a timestamp helper, and keep the
  timestamp in a wrapper node so the validated comparison payload stays unchanged.
- Canvas port colors should not be the same thing as semantic `PortSpec.portColor` overrides. A
  type-only fallback makes same-type workflow values look duplicated across the canvas, even when
  the model is correct. Rule: keep explicit port colors for compatibility channels, but derive the
  visual dot color from the node instance when a port does not opt into a fixed semantic color.
