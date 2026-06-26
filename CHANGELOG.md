# Changelog

All notable changes to Graphyn are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.7.0] — 2026-06-26

### Fixed

- **publishing:** Maven Central deployments never reached `repo1.maven.org` since v0.3.0 — three compounding bugs fixed:
  - `automaticRelease = true` now set on every module (deployments previously sat PENDING in the Central Portal forever)
  - Signing condition checks `signingInMemoryKey` (the property CI actually sets) instead of the never-set `signingKey` — `.asc` signatures are now generated
  - Migrated to vanniktech maven-publish `0.37.0` (removed `SonatypeHost`, enabled Dokka v2 mode)
- **publishing:** POM leaks fixed — unpublished modules (`core:designsystem`) demoted from `api()` to `implementation()` in `app:shared` and `ui:cards`
- **plugins:preview:** `MediaOutputCardPlatform` had no `actual` for native/JS/WASM/Android — added a multiplatform `MediaOutputPlaceholderCard` fallback so `graphyn-runtime` and `graphyn-editor` publish for all KMP targets

### Added

- **ai:** `graphyn-ai` first published to Maven Central (`io.github.ronjunevaldoz:graphyn-ai:0.7.0`)
- **runtime:** `graphyn-runtime` first published to Maven Central (`io.github.ronjunevaldoz:graphyn-runtime:0.7.0`)
- **docs:** Maven publishing rules added to `CLAUDE.md` (automaticRelease, signing property, `api()`/`implementation()` POM hygiene)

---

## [0.6.0] — 2026-06-26

### Features

- **server:** `install(Graphyn)` Ktor plugin — embed the full workflow API into any existing Ktor server with a single call
- **server:** `graphyn-server` first published to Maven Central (`io.github.ronjunevaldoz:graphyn-server:0.6.0`)
- **server:** `GraphynKtorConfig` — typed config: `routePrefix`, `requireApiKey`, `apiKey`, `store`, `extraPlugins`
- **server:** `GraphynAuthPlugin` now accepts explicit `apiKey` via config (falls back to `GRAPHYN_API_KEY` env var)

### Security

- **app:** Replaced hardcoded personal home-server URL with `GRAPHYN_OLLAMA_HOST` env var (default: `http://localhost:11434`)

### Documentation

- `docs/guides/server-embedding.md` — quick-start, config reference, routes, auth, custom plugins, SSE example
- `docs/reference/compatibility-matrix.md` — artifact table, KMP target matrix, version history, tool versions

---

## [0.5.0] — 2026-06-26

### Features

- **media:** Phase 2 captioning & composition nodes (`speech_to_text`, `ocr`, `caption_overlay`, `video_compose`, `timing_controller`, `image_import`) + 4 new templates
- **media:** Phase 3 image ops (`image_resize`, `image_crop`, `images_list`, `image_sequence_to_video`) + Image Edit + Slideshow templates
- **media:** `media.audio_encode` node — audio templates now terminate in `media.file_output` instead of `preview.view`
- **media:** Zero-config TTS/OCR fallbacks (`say` on macOS, `tesseract`) when `GRAPHYN_*_EXECUTABLE` env vars unset
- **app:** Production app reframe — `app/demo → app/app`, `DemoApp → GraphynApp`, `DemoScene → WorkflowCatalog`
- **launcher:** Platform-gated template catalog — `catalogTemplatesFor(nodeSpecs)` hides JVM-only templates on Web

### Refactoring

- **media-core:** Split `FfmpegMediaCoreBackend` into `FfmpegDecode`, `FfmpegEncode`, `FfmpegImage`, `MediaCompositionFilters` (all under 150-line ceiling)

---

## [0.4.1] — 2026-06-26

### Bug Fixes

- **cards:** Support editable structured fields

## [0.4.0] — 2026-06-26

### Bug Fixes

- **editor:** Persist layouts and streamline launcher

### CI

- Run core submodule checks instead of removed :core aggregate
- **publish:** Grant contents:write so the release step can create the GitHub Release

### Documentation

- **lessons:** Record canvas-geometry, annotation-executor, and unmerged-tree findings

### Features

- **media:** Add media workflow modules and fix desktop datetime runtime
- **media:** Workflow templates, path resolver, and output preview
- **templates:** Per-template guides, output previews, and auto-layout on load
- **media:** Phase 2 captioning & composition nodes
- **media:** Phase 2.5 — image_import node + captioning demo template

### Refactoring

- **editor:** Retire GraphynNodeCard for a default FieldCardFactory
- **cards:** Refine field card inputs and record editing

## [0.3.0] — 2026-06-25

### Bug Fixes

- Resolve ShapeCardFactory signature mismatch and add WASM JS support
- Remove ShapeNodeTheme instantiation causing NoSuchMethodError
- **plugins:** Apply Compose compiler plugin to gmail and linkedin
- **ui/cards:** Align ShapeCard shape with its port anchors
- **plugins:** Assign categories to gmail/linkedin specs so they group in palette
- **palette:** Replace JVM-only toSortedMap() with commonMain-safe sort

### Build

- **core:** Publish each core submodule to Maven Central
- **release:** Prep v0.3.0 — fix publish workflow for split core
- **release:** Publish ui:cards in the Maven Central workflow

### Documentation

- Simplify README, update architecture diagram, bump version to 0.2.1
- Add React Native + Flutter plugin roadmap items
- Add LinkedIn + Twitter/X integration plan
- **readme:** Reflect core split in README and core/README
- **linkedin:** Mark the LinkedIn plugin clearly as a sample

### Features

- **plugins/linkedin:** Add LinkedIn integration with 7 node specs
- **plugins:** Integrate Gmail and LinkedIn into demo app
- **palette:** Neutralize node browser colors + nest categories into folders
- **palette:** Organize categories into folders (Data, Flow, Creative)
- **plugins:** Make Gmail functional (base64url) + wireable outputs for gmail/linkedin
- **gmail:** Resolve credentials from platform storage by reference

### Refactoring

- **plugins:** Move gmail/linkedin editor plugins to commonMain
- **core:** Split core into model/execution/serialization/data submodules
- **core:** Move single-concern tests into their submodules
- **api:** Narrow editor-api/plugin-api core dependency to model+execution
- **core:** Remove :core aggregator — core is folder-only

## [0.2.1] — 2026-06-24

### Bug Fixes

- **cards:** Boolean toggle, wider fields, fix list-item edit state reset
- **cards:** Move drag handle to FieldCard header; retire DarkHeaderCard; add Script node
- **script:** Make ScriptCard draggable from any non-editor area
- **autolayout:** Wire fitToContent after AutoLayout, fix race with canvasCards
- **picker:** Exclude OpaqueType ports from node picker suggestions
- **execution:** Route subgraph output through registered executor to avoid port-key leak
- **audit:** Resolve all 5 GitHub issues from codebase audit
- **audit:** Replace hardcoded dp literals with design tokens, add missing plugin tests
- **minimap,layout:** Restore * 2f node scale, widen auto layout gaps
- **layout:** Thread auto-layout positions+sizes directly into fitToContent
- **viewport:** Fit-to-content scale floor + auto-refit on resize
- **ai:** Parse Ollama responses as NDJSON; docs + live integration test
- **build:** Disable gradle configuration cache to avoid instrumentation corruption

### Build

- Skip signing when key is absent (local publish support)
- Doppler-backed Maven Central publish script
- Support DOPPLER_TOKEN in publish script
- Load DOPPLER_TOKEN from .env file
- **release:** Git-cliff changelog + GitHub release in publish script
- Auto-bump patch version after publish + fix stale fallbacks

### CI

- Temporarily skip WasmJS build due to Kotlin compiler issue
- Remove --strict flag from mkdocs build
- Skip API docs copy pending Dokka output fix

### Documentation

- **api:** KDoc on all public API surfaces + card factory cleanup
- Clarify app module purposes + fix stale quickstart
- Update lessons, coverage matrix, README for Script node session
- Capture executor-resilience contract + nullable/required port lessons
- Capture serialization-plugin-per-module and SSE single-line lessons
- **lessons:** M2 learnings — @Serializable on model types, kotlinx-datetime for KMP clock
- **lessons:** WasmJs localStorage interop and web.window matchMedia fix
- **readme:** Reflect 0.2.0 features — persistence, parallel exec, server API, demo scenes
- Update README + lessons for executeAsFlow and inline widgets
- Add missing integration, extending, and reference guides to navigation
- **lessons:** Document Kotlin 2.4.0 WasmJS IR deserialization bug
- Kotlin bug report template for WasmJS IR deserialization issue
- Remove kotlin bug report template (tracked in issue #6 instead)

### Features

- **layout+nav:** Size-aware auto-layout, canvas centering, home navigation
- **io:** Real HTTP via Ktor, async NodeExecutor
- **sample:** Add consumer sample app + update docs for 0.2.0
- **output-panel:** Collapsible Output/Logs panel with auto-open on run
- **desktopApp:** Wire ScriptPlugin into the official desktop app
- **desktopApp:** Wire ScriptPlugin into the official desktop app
- **cards,script:** Larger nodes + IDE-style code editor for Script node
- **preview,script:** Preview.view node + Kotlin syntax highlighting
- **demo:** Script scene wires preview.view and centers nodes on canvas
- **io:** Add io.file_browse node with KMP file/folder picker
- **docs:** Deploy WasmJS demo + Dokka API reference to GitHub Pages
- **minimap:** Fade in on camera change, fade out after idle, fill viewport rect
- **minimap,layout:** Minimap world padding, fix node * 2f scale, improve auto layout
- **layout:** Dynamic auto-layout gaps + subgraph Roborazzi test
- **execution:** Resilient executor v2 — per-node isolation + live events
- **json:** JSON plugin — parse / stringify / path
- **demo:** API ingestion sample workflow + fix http_request optional ports
- **runtime:** Shared :runtime module — one production plugin set for all hosts
- **server:** M3 execution API — validate, async runs, SSE event streaming
- **store:** M2 — WorkflowStore with versioning, diffs, and auto-save
- **m5:** Wire WorkflowStore into launcher and desktop app shell
- **m4:** Real file I/O, webhook POST, env reader, timeout+retry
- **m6:** Parallel node execution, server auth, concurrency limit, CI
- **webstore:** LocalStorageWorkflowStore for wasmJs + JS web targets
- Server /workflows CRUD API + AI/Geometry/Automation demo scenes
- ExecuteAsFlow + ShapeCard inline config widgets
- JVM/Android plugin auto-discovery + subgraph input injection
- Collapse selection into subgraph + expand, with derived boundary specs
- Styled subgraph node card with double-click drill-in
- Keyboard shortcut configuration — data model, state, persistence
- Wire configurable shortcuts into editor — gesture + toolbar UI
- Ai/ module — LLM workflow generation (Ollama + placeholder)
- New-workflow dialog with AI generation
- **ai:** Fill node input config during generation
- **ai:** Docked AI assistant panel with chat history; drop launcher modal
- **plugins:** Gmail integration nodes with credential management pattern
- **plugins/gmail:** Integrate with editor via GmailPlugin and GmailEditorPlugin

### Refactoring

- **demo:** Consolidate scene workflows, remove orphaned data
- **io:** Split io.file_browse into io.file_browse + io.folder_browse, update subgraph demo
- **M0:** Bidirectional OpaqueType + split GraphynEditorState under 150
- **ui:** Centralize spacing tokens in core/designsystem

### Reverts

- **cards:** Restore FieldCard to original dimensions

### Testing

- **demo:** Add DemoSceneWorkflowTest covering all 8 scene templates
- **minimap:** Make camera-outline test deterministic

## [0.2.0] — 2026-06-19

### Bug Fixes

- **canvas:** Remove double scale division in node drag
- **sample-logger-ui:** Replace Material3 Card/Text with BasicText
- **style-nodes:** Add port type colour dots to DarkHeaderCard and FieldCard
- **publish:** Add publishToMavenCentral + signAllPublications to all modules
- **publish:** Disable configuration cache — incompatible with vanniktech publish tasks
- **style-nodes:** Remove duplicate port dots; divider edge-to-edge
- **style-nodes:** Pin header/row/divider to explicit heights for accurate port anchors
- **ui-cards:** Guard inline field editing against spurious focus-lost commit
- **ui-cards:** Cap dropdown popup width to 160dp, add maxLines=1 on options
- **ui-cards:** Uniform VALUE_DP=80 width for all FieldCard value widgets
- **core:** OpaqueType accepts any incoming connection type
- **canvas+palette:** Sticky note z-order/resize/minimap, tree auto-layout, palette collapsed by default, execution outputs in inspector, shell file-size split
- **demo:** Make subgraph node draggable and show inner contents

### Documentation

- **design:** Add node design research — Graphyn vs ComfyUI vs Blender vs n8n
- **design:** Embed interactive node comparison widget in research doc
- Add README to every module
- **plans:** Add demo-scenarios analysis and proposal
- **readme:** Full external consumer guide — ShapeCardFactory, onConfigChange, workflowFlow
- **readme:** Add graphyn-ui-cards to module table, installation, and quick start
- Node UI guide, KDoc on public API surface, update roadmap

### Features

- **canvas:** Port type colour coding + 7-editor design comparison
- **plugins:** Style-nodes PoC — ComfyUI, Blender, n8n custom canvas cards
- **style-nodes:** Neutral card names, per-port colors, wasmJs fixes
- **minimap:** Per-node shape/size, canvas aspect ratio, rectangle shape
- **core:** Expose WorkflowSerializer public API with tests and benchmarks
- **editor:** Execution status badge on all plugin card styles
- **palette:** Search by type + distinct empty states
- **minimap:** Add border + inner padding consistent with zoom controls
- **spm:** Add Package.swift + auto-patch xcframework workflow
- **demo:** Wire WorkflowExecutionEngine to DemoApp
- Auto-layout, node category palette, card uniformity, drag fix
- **api:** External consumer write-back + workflow observation
- **style-nodes:** CircleCardFactory — configurable theme + avatar slot
- **style-nodes:** FieldCardFactory — merged port rows, header/body/footer layout
- **ui/cards:** Wire theme defaults to AppTheme — adapts to light/dark mode
- **ui/cards:** Inline field editing via NodeCanvasContext.onConfigChange
- **ui-cards:** Add single-select and multi-select dropdown rows to FieldCard
- **ui-cards:** Numeric stepper, type-gated input validation, auto-layout skip
- **ui-cards:** ListType port row with add/remove popup
- **ui-cards:** RecordType port row with per-field popup editor
- **core+ui:** NodeSpec.description shown in inspector and palette
- **core+ui:** PortSpec.description shown in inspector ports section
- Canvas annotations, group nodes, 5 first-party plugins, demo scenes, SaaS docs
- **demo:** Groups and Subgraph demo scenes
- **subgraph+ui:** Drill-in navigation, launcher, collapsible run results
- **subgraph+canvas:** Nested results, position persistence, canvas overlays, center node placement

### Refactoring

- **inspector:** Split into 5 files + design polish
- **demo:** Extract app/demo module — remove bootstrap from published library
- **style-nodes:** Generalize CircleCardFactory → ShapeCardFactory
- **style-nodes:** Split FieldCard body/footer — inputs in body, outputs in footer
- Extract ShapeCardFactory + FieldCardFactory into ui/cards module

### Testing

- **core+shared:** Config value change propagation and editor state tests

## [0.1.0] — 2026-06-18

### Bug Fixes

- **editor:** Port positioning, delete node, and preview infrastructure
- **tests:** Resolve 3 pre-existing test failures
- **canvas:** Correct port dot z-order and px density; add surface UI tests
- **drag:** Eliminate initial-drag blink on node cards
- **canvas:** Node drag jump + grid visual overhaul
- **ci:** Add core/designsystem module to repo — was accidentally gitignored

### Documentation

- Add architecture and plan drafts
- Add customization quick start
- Draft plugin api
- Add phased task checklist
- **architecture:** Add test coverage matrix

### Features

- Add core workflow model and registries
- Add library editor core and benchmarks
- Add phase 2 canvas scaffold
- Add phase 3 editor interactions
- Add editor interaction intents
- Add mvp workflow execution
- Add plugin api module
- Wire app plugins into editor
- Add sample logger plugin
- Make host apps demo-ready
- Add sample editor panel
- Add editor plugin api
- Centralize demo bootstrap
- Make demo bootstrap overridable
- Preload demo workflow
- Polish demo canvas and theme controls
- Add node creation and theme persistence
- Refine minimap bounds and camera
- Extract canvas components and add validation
- Stabilize backdrop and editor panel host
- **editor:** Delete selected connection via inspector panel
- **editor:** Phase 4.5 + 4.6 — gesture polish, keyboard shortcuts, node picker
- **plugins:** Add sample-math plugin + complete MVP phases 5 & 6
- **mvp:** CI pipeline, README quickstart, and @GraphynExperimentalApi audit
- **publish:** Wire Maven Central publishing via vanniktech plugin
- **editor:** Implement tasks 10-13 — type validation, execution badges, serializer API, palette search
- **publish:** Wire graphyn-editor artifact + XCFramework packaging for release
- **docs:** MkDocs site with GitHub Pages deployment

### Refactoring

- **state:** Split GraphynEditorState 361→196 lines into focused sub-states
- **audit:** Migrate off MaterialTheme + enforce 150-line ceiling

### Testing

- **drag:** Add two-node independent drag verification + Roborazzi baseline
