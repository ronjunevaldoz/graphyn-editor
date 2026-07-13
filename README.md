<p align="center">
  <img src="docs/logo.svg" alt="Graphyn" width="280"/>
</p>

<p align="center">
  <strong>Node-based workflow editor for Kotlin Multiplatform</strong><br/>
  Drop a canvas into any app — AI pipelines, shader graphs, automation builders, game tools.
</p>

<p align="center">
  <a href="https://github.com/ronjunevaldoz/graphyn-editor/actions/workflows/ci.yml">
    <img src="https://github.com/ronjunevaldoz/graphyn-editor/actions/workflows/ci.yml/badge.svg" alt="CI"/>
  </a>
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Compose-Multiplatform-3DDC84?logo=jetpackcompose&logoColor=white" alt="Compose Multiplatform"/>
  <img src="https://img.shields.io/badge/platforms-Android%20·%20Desktop%20·%20Web%20·%20iOS-0095D5" alt="Platforms"/>
  <img src="https://img.shields.io/badge/release-0.9.1-blue" alt="Release"/>
</p>

---

## What is Graphyn?

Graphyn is a **Kotlin Multiplatform library** that gives your app a fully-featured node-based canvas — the kind used in Unreal Blueprint, Blender shader editor, or n8n. Wire it up with plugins to define your own node types, connect them, and execute workflows.

- **Library-first** — embed the canvas in your own app, not the other way around
- **Plugin architecture** — runtime plugins define node specs and executors; editor plugins add custom card UI
- **Compose Multiplatform UI** — one codebase for Android, Desktop (JVM), Web (Wasm/JS), and iOS
- **MVI state model** — all canvas mutations go through typed intents; easy to test and replay
- **Zero Material dependency** — custom design system, fully themeable

**Docs site:** [ronjunevaldoz.github.io/graphyn-editor](https://ronjunevaldoz.github.io/graphyn-editor)

---

## Features

- **Pan & zoom** with minimap
- **Node editor** — drag ports to connect, split/merge workflows
- **Plugin system** — runtime plugins (node specs + executors) and editor plugins (custom UI)
- **Parallel execution** — independent nodes run concurrently
- **Live streaming** — `executeAsFlow()` for real-time progress
- **Persistence** — auto-save to file (JVM) or local storage (Web)
- **AI generation** — describe a workflow, Ollama drafts it onto the canvas
- **Customizable shortcuts** — rebind any editor action from the toolbar
- **Subgraph collapse** — group nodes into reusable workflows
- **Inline widgets** — ComfyUI/Blender-style config cards
- **Theme support** — light/dark mode + custom colors
- **TypeScript-safe ports** — validation at wire time

---

## Architecture

```
        ┌─────────────────────────────────┐
        │   app/shared (Compose UI)       │
        │     Canvas + Editor Shell       │
        └────────┬──────────────┬─────────┘
                 │              │
         ┌───────▼────┐  ┌──────▼──────┐
         │ editor-api │  │ plugin-api  │
         │ Card UI    │  │ Node specs  │
         │ Panels     │  │ Executors   │
         └───────┬────┘  └──────┬──────┘
                 │              │
          ┌──────▼───────────────▼──────┐
          │   core/  (folder of modules) │
          │  model · execution           │
          │  serialization · data        │
          └──────────────────────────────┘

ui/cards (ShapeCardFactory, FieldCardFactory)
plugins/* (node definitions + executors)
```

`core` is a folder of focused, layered modules — there is no umbrella `:core` module. Each consumer depends only on the submodules it uses; everything builds up from `core:model`.

| Module | Artifact | What |
|---|---|---|
| `core:model` | `graphyn-core-model` | Workflow model, types, validation, registry |
| `core:execution` | `graphyn-core-execution` | Execution engine, executors, events |
| `core:serialization` | `graphyn-core-serialization` | Workflow document codec |
| `core:data` | `graphyn-core-data` | Workflow stores + platform persistence |
| `core:designsystem` | `graphyn-ui-design` | Design tokens, theme, and UI primitives |
| `editor-api` | `graphyn-editor-api` | Canvas card + panel contracts |
| `plugin-api` | `graphyn-plugin-api` | Node spec + executor contracts |
| `ui/cards` | `graphyn-ui-cards` | Ready-made card shapes (Shape, Field, Circle) |
| `app/shared` | `graphyn-editor` | Compose Multiplatform canvas + editor shell |
| `ai` | `graphyn-ai` | LLM workflow generation (Ollama) |
| `runtime` | `graphyn-runtime` | Convenience bundle of all first-party plugins |
| `server` | `graphyn-ktor-plugin` | Ktor execution API + `install(Graphyn)` |
| `mcp` | — (application) | Stdio MCP server — generic workflow CRUD + execute for agents |
| `plugins/control` | `graphyn-plugin-control` | Branch, loop, merge |
| `plugins/list-ops` | `graphyn-plugin-list-ops` | Map, filter, reduce, sort |
| `plugins/types` | `graphyn-plugin-types` | Type conversion and casting |
| `plugins/text` | `graphyn-plugin-text` | Split, join, replace, template |
| `plugins/io` | `graphyn-plugin-io` | HTTP, file read/write, path resolution |
| `plugins/json` | `graphyn-plugin-json` | JSON parse, query, transform |
| `plugins/preview` | `graphyn-plugin-preview` | Live output preview nodes |
| `plugins/sticky-notes` | `graphyn-plugin-sticky-notes` | On-canvas annotation nodes |
| `plugins/script` | `graphyn-plugin-script` | Kotlin Script eval (JVM) |
| `plugins/media-core` | `graphyn-plugin-media-core` | FFmpeg-backed media processing (JVM) |
| `plugins/media-ai` | `graphyn-plugin-media-ai` | TTS / STT / OCR adapters (JVM) |
| `plugins/gmail` | `graphyn-plugin-gmail` | Gmail integration (fetch, send, reply) |
| `plugins/linkedin` | `graphyn-plugin-linkedin` | LinkedIn profile and feed nodes |

---

## Installation

```kotlin
// gradle/libs.versions.toml
[versions]
graphyn = "0.9.1"

[libraries]
graphyn-editor        = { module = "io.github.ronjunevaldoz:graphyn-editor",              version.ref = "graphyn" }
graphyn-ui-cards      = { module = "io.github.ronjunevaldoz:graphyn-ui-cards",            version.ref = "graphyn" }
graphyn-runtime       = { module = "io.github.ronjunevaldoz:graphyn-runtime",             version.ref = "graphyn" }
graphyn-plugin-api    = { module = "io.github.ronjunevaldoz:graphyn-plugin-api",          version.ref = "graphyn" }
graphyn-editor-api    = { module = "io.github.ronjunevaldoz:graphyn-editor-api",          version.ref = "graphyn" }
graphyn-core-model    = { module = "io.github.ronjunevaldoz:graphyn-core-model",          version.ref = "graphyn" }
graphyn-plugin-io     = { module = "io.github.ronjunevaldoz:graphyn-plugin-io",           version.ref = "graphyn" }
graphyn-plugin-gmail  = { module = "io.github.ronjunevaldoz:graphyn-plugin-gmail",        version.ref = "graphyn" }
# full plugin list: graphyn-plugin-{control,list-ops,types,text,io,json,preview,sticky-notes,script,media-core,media-ai,gmail,linkedin}
# full core list:   graphyn-core-{model,execution,serialization,data}
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.graphyn.editor)   // full canvas UI
    implementation(libs.graphyn.runtime)  // all first-party plugins bundled
}
```

| Use case | Dependency |
|---|---|
| Full canvas editor | `graphyn-editor` |
| All first-party plugins | `graphyn-runtime` |
| Card UI kit only | `graphyn-ui-cards` |
| Build a runtime plugin | `graphyn-plugin-api` |
| Build an editor plugin | `graphyn-editor-api` |
| Individual plugin | `graphyn-plugin-{control,io,json,…}` |
| Workflow model only | `graphyn-core-model` (+ `-execution` / `-serialization` / `-data` as needed) |

---

## Quick Start

A runtime plugin defines a node's spec (ports, defaults) and executor (the actual logic):

```kotlin
object MyPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(id = "com.example.my", displayName = "My Plugin", version = "1.0.0")

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(
            NodeSpec(
                type = "my.transform",
                label = "Transform",
                category = "com.example.text",
                inputs  = listOf(PortSpec("input",  WorkflowType.String)),
                outputs = listOf(PortSpec("output", WorkflowType.String)),
                defaultValues = mapOf("input" to WorkflowValue.StringValue("hello")),
            )
        )
        registrar.registerExecutor("my.transform") { inputs ->
            val value = (inputs["input"] as? WorkflowValue.StringValue)?.value ?: ""
            mapOf("output" to WorkflowValue.StringValue(value.uppercase()))
        }
    }
}
```

Wire it into the editor shell:

```kotlin
@OptIn(GraphynExperimentalApi::class)
@Composable
fun App() {
    val runtimeRegistry = remember { DefaultGraphynPluginRegistry().apply { install(MyPlugin) } }
    val state = rememberGraphynEditorState()

    GraphynEditorShell(
        state = state,
        dependencies = GraphynEditorShellDependencies(nodeSpecs = runtimeRegistry.nodeSpecs),
        branding = GraphynBranding(appName = "My Studio"),
    )
}
```

For a custom canvas card, inspector panel, `FieldCard`'s per-`WorkflowType` input widgets, auto-discovery via `ServiceLoader`, subgraphs, AI workflow generation, and keyboard shortcuts — see the [docs site](https://ronjunevaldoz.github.io/graphyn-editor) and [Plugin API](./docs/architecture/plugins.md).

---

## Plugin Model

Graphyn separates runtime concerns from editor concerns — you can ship a runtime plugin without any UI dependency.

| Layer | Module | Stable contract |
|---|---|---|
| Runtime | `plugin-api` | `GraphynPlugin`, `GraphynPluginRegistrar`, `NodeSpec`, `NodeExecutor` |
| Editor cards | `editor-api` | `NodeCanvasFactory`, `GraphynEditorPluginRegistrar` |
| Editor panels | `editor-api` | `EditorPanelFactory`, `EditorPanelContext` (includes `onConfigChange`) |
| State observation | `editor-api` | `GraphynEditorStateView` — `workflow`, `workflowFlow: StateFlow`, `selectedNodeId` |

`Default*` implementations are marked `@GraphynExperimentalApi` — their signatures may evolve. The interfaces above are stable.

### Auto-discovery (JVM / Android)

Instead of wiring every plugin by hand, hosts on the JVM and Android can let plugins register themselves via `java.util.ServiceLoader`. A plugin author ships a resource file:

```
# src/main/resources/META-INF/services/com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
com.example.MyPlugin
```

Then the host installs everything on the classpath in one call:

```kotlin
val registry = DefaultGraphynPluginRegistry().apply {
    install(CorePlugin)   // explicit, always present
    installDiscovered()   // plus any plugin on the classpath (ServiceLoader)
}
```

`installDiscovered()` skips plugins already installed (matched by `metadata.id`) and returns the newly-installed ones. On JS, Wasm, and iOS it is a no-op — those hosts register plugins explicitly.

### Subgraphs

A node may embed a nested `WorkflowDefinition` via `NodeRef.subgraph`. The engine runs it recursively (and in parallel where possible), and the subgraph node's resolved inputs are injected into the inner workflow's **free input ports** (those with no internal connection or config), keyed by port name — so a parent workflow can feed data into a nested one. Explicit inner config and internal wiring always take precedence. A subgraph node's outputs are the inner workflow's **free output ports** (those nothing inside consumes).

In the editor, select two or more nodes and press **Cmd/Ctrl + Shift + G** to collapse them into a subgraph node; its boundary ports are derived automatically from the inner workflow. **Double-click** a subgraph node (or use **Enter →** in the inspector) to drill into it, and **Expand ⤢** in the inspector to inline it again.

### AI workflow generation

The `:ai` module turns a natural-language prompt into a `WorkflowDefinition`. `WorkflowGenerator` has two implementations:

- `OllamaWorkflowGenerator(OllamaConfig(baseUrl, model))` — calls an Ollama host's `/api/generate` with `format=json`. The prompt embeds the node catalog with **port types** (`type — [in:type] -> [out:type]`) so the model only emits real node types and fills each node's `config` with type-matched literals for every unconnected input; `WorkflowJsonParser` validates the output, coerces config values to each port's `WorkflowType`, and drops unknown node types / dangling connections rather than failing. Default model `qwen2.5-coder:14b`.
- `PlaceholderWorkflowGenerator` — offline canned output for UI development and no-host scenarios.

The editor surfaces this as a **docked ✨ AI panel** (toggle from the toolbar). Describe a workflow and it generates onto the current canvas, auto-laid-out. The panel keeps a **chat transcript** so you can iterate, and each result reports what was created plus any **unsupported nodes or dropped connections** that were sanitized away — so you learn why the graph differs from your request. Generation failures stay inline and retryable.

### Configurable keyboard shortcuts

Every editor action (undo, copy, group, collapse, …) is a rebindable `EditorShortcutAction`. Open **⌨ Keys** in the toolbar, click an action's chord chip, and press the new combination — bindings persist via the settings store and conflicts are rejected. Contextual keys (Escape, Delete) are fixed. See [keyboard-shortcuts.md](docs/reference/keyboard-shortcuts.md).

---

## Platform Targets

| Platform | Status |
|---|---|
| Android | ✅ |
| Desktop (JVM) | ✅ |
| Web (Wasm) | ✅ |
| Web (JS) | ✅ |
| iOS | ✅ XCFramework (SPM) |
| Server (JVM) | ✅ runtime only |

---

## Running

```bash
./gradlew :app:desktopApp:run                          # Desktop
./gradlew :app:androidApp:assembleDebug                # Android
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun      # Web (Wasm)
./gradlew :app:webApp:jsBrowserDevelopmentRun          # Web (JS)
```

---

## Server API

Start the Ktor server:

```bash
./gradlew :server:run
```

The server exposes:

| Endpoint | Description |
|---|---|
| `GET /` | Health check |
| `GET /nodes` | List all registered node specs |
| `GET /nodes/{type}` | Get a single node spec by type (404 if unknown) |
| `POST /validate` | Validate a workflow — returns `[ValidationError]` |
| `POST /execute` | Run synchronously — returns `WorkflowExecutionResult` |
| `POST /executions` | Start async run — returns `{ runId }` (202) |
| `GET /executions/{id}/events` | SSE stream of per-node events + final result |
| `GET /workflows` | List persisted workflows (newest first) |
| `GET /workflows/{id}` | Load a workflow by ID |
| `POST /workflows` | Save / upsert a workflow (returns `WorkflowMeta`, 201) |
| `DELETE /workflows/{id}` | Delete workflow and its version history |

Set `GRAPHYN_API_KEY=<secret>` in the environment to enable Bearer-token auth. All endpoints except `GET /` require the token when the env var is present.

---

## MCP Server

`:mcp` is a stdio [MCP](https://modelcontextprotocol.io) server for agents (Claude Desktop, Claude Code, etc.) — generic CRUD + execute over workflows, no template-specific tools. It embeds the engine in-process (`FileWorkflowStore`, defaults to `<project-root>/.graphyn/workflows` — override with `GRAPHYN_MCP_WORKFLOWS_DIR`), no running `:server` needed.

```bash
./gradlew :mcp:installDist
```

Add it to your MCP client config, e.g. a project's `.mcp.json`:

```json
{
  "mcpServers": {
    "graphyn-workflows": {
      "command": "./mcp/build/install/mcp/bin/mcp",
      "args": []
    }
  }
}
```

| Tool | Description |
|---|---|
| `workflow_list` | List all stored workflows |
| `workflow_get` | Fetch a workflow's full definition by id |
| `workflow_publish` | Save/update a workflow from raw JSON (validates first, id must start with `mcp-`) |
| `workflow_delete` | Delete a workflow and its history (id must start with `mcp-`) |
| `workflow_execute` | Run a stored workflow by id, with optional `overrides` and `async` |
| `workflow_execution_status` | Poll progress for an `async` run |
| `workflow_list_node_types` | List registered node types, for authoring `workflow_publish` payloads |

By default `:mcp` installs the Shorts, MediaCore, MediaAi, and StableDiffusion plugins on top of the base runtime set. Trim or reorder with `GRAPHYN_MCP_PLUGINS` (comma-separated, or `all`):

```json
"env": { "GRAPHYN_MCP_PLUGINS": "shorts,media-core" }
```

`workflow_publish`/`workflow_delete` are scoped to ids starting with `mcp-` — they share `:mcp`'s own store with the desktop editor, and this stops an agent from overwriting or deleting your real workflows by reusing an id. `workflow_publish`/`workflow_execute` run against the real engine — nodes like `script.eval` and `io.file_write`/`io.http_request` execute unsandboxed. Tool annotations (`destructiveHint`/`openWorldHint`) flag which ones.

---

## Publishing

Artifacts are published to Maven Central automatically when a version tag is pushed (`git tag vX.Y.Z && git push origin vX.Y.Z`). For local / manual publishing without CI, use the Doppler-backed script:

```bash
# Prerequisites: doppler CLI + DOPPLER_TOKEN in .env or .env.local
# Doppler project "maven-central / prd" must have MAVEN_CENTRAL_USERNAME,
# MAVEN_CENTRAL_PASSWORD, GPG_SIGNING_KEY, GPG_SIGNING_PASSWORD

# Publish all modules at version from gradle.properties
./scripts/publish-local.sh

# Explicit version
./scripts/publish-local.sh 0.7.4

# Single module only (fastest for hotfixes)
./scripts/publish-local.sh 0.7.4 server
```

See [`.env.example`](.env.example) for all supported environment variables and [`docs/reference/compatibility-matrix.md`](docs/reference/compatibility-matrix.md) for the full artifact list.

---

## Testing

```bash
# core is a folder of submodules — check each (core:execution holds the integration test)
./gradlew :core:model:check :core:execution:check :core:serialization:check :core:data:check
./gradlew :app:shared:jvmTest         # canvas + editor UI tests (Roborazzi)
./gradlew :plugins:io:jvmTest         # I/O plugin tests
./gradlew :server:test                # server route tests
```

---

## Docs

Full documentation at [ronjunevaldoz.github.io/graphyn-editor](https://ronjunevaldoz.github.io/graphyn-editor).

- [Architecture overview](./docs/architecture/README.md)
- [Plugin API](./docs/architecture/plugins.md)
- [Core API](./docs/architecture/core-api.md)
- [Engineering lessons](./docs/architecture/lessons.md)
- [Test coverage](./docs/architecture/test-coverage.md)

---

## iOS (SPM)

Add the package in Xcode → **File → Add Package Dependencies**:

```
https://github.com/ronjunevaldoz/graphyn-editor
```

Or in `Package.swift`:

```swift
.package(url: "https://github.com/ronjunevaldoz/graphyn-editor", from: "0.1.0")
```

---

## License

Apache 2.0 © [Ron June Valdoz](https://github.com/ronjunevaldoz)
