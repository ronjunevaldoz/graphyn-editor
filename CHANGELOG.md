# Changelog

All notable changes to Graphyn are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

### Build

- Skip signing when key is absent (local publish support)
- Doppler-backed Maven Central publish script
- Support DOPPLER_TOKEN in publish script
- Load DOPPLER_TOKEN from .env file

### Documentation

- **api:** KDoc on all public API surfaces + card factory cleanup
- Clarify app module purposes + fix stale quickstart
- Update lessons, coverage matrix, README for Script node session
- Capture executor-resilience contract + nullable/required port lessons
- Capture serialization-plugin-per-module and SSE single-line lessons
- **lessons:** M2 learnings — @Serializable on model types, kotlinx-datetime for KMP clock

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

### Refactoring

- **demo:** Consolidate scene workflows, remove orphaned data
- **io:** Split io.file_browse into io.file_browse + io.folder_browse, update subgraph demo
- **M0:** Bidirectional OpaqueType + split GraphynEditorState under 150

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


