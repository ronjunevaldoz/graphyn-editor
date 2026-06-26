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
  <img src="https://img.shields.io/badge/release-0.6.0-blue" alt="Release"/>
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

| Module | What | Status |
|---|---|---|
| `core:model` | Workflow model, types, validation, registry, `NodeGroups` | Library |
| `core:execution` | Execution engine, executors, events | Library |
| `core:serialization` | Workflow document codec | Library |
| `core:data` | Workflow stores + platform persistence | Library |
| `editor-api` | Canvas card + panel contracts (→ core:model, core:execution) | Library |
| `plugin-api` | Node spec + executor contracts (→ core:model, core:execution) | Library |
| `ui/cards` | Ready-made card shapes | Library |
| `app/shared` | Canvas + editor UI | App |
| `plugins/gmail` | Gmail integration (fetch, send, reply) | 0.3.0 ✅ |
| `plugins/linkedin` | LinkedIn nodes — only `GET /v2/me` is a live call; rest are placeholder endpoints | Sample |
| `plugins/io` | HTTP, file, env, webhook | Sample |
| `plugins/control` | Branch, loop, merge | Sample |
| `plugins/text` | Format, split, regex | Sample |
| `app/desktopApp` | Desktop editor (JVM) | App |
| `server` | Ktor execution API | App |

---

## Installation

```kotlin
// gradle/libs.versions.toml
[versions]
graphyn = "0.3.0"

[libraries]
graphyn-editor     = { module = "io.github.ronjunevaldoz:graphyn-editor",         version.ref = "graphyn" }
graphyn-ui-cards   = { module = "io.github.ronjunevaldoz:graphyn-ui-cards",       version.ref = "graphyn" }
graphyn-plugin-api = { module = "io.github.ronjunevaldoz:graphyn-plugin-api",     version.ref = "graphyn" }
graphyn-core-model = { module = "io.github.ronjunevaldoz:graphyn-core-model",     version.ref = "graphyn" }
# core is published per-submodule: graphyn-core-{model,execution,serialization,data}
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.graphyn.editor)      // Full canvas UI
}
```

| Use case | Dependency |
|---|---|
| Full canvas editor | `graphyn-editor` |
| Card UI kit | `graphyn-ui-cards` |
| Build a runtime plugin | `graphyn-plugin-api` |
| Build an editor plugin | `graphyn-editor-api` |
| Workflow model only | `graphyn-core-model` (+ `-execution` / `-serialization` / `-data` as needed) |

---

## Quick Start

### Step 1 — Runtime plugin (node specs + executors)

```kotlin
object MyPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "com.example.my",
        displayName = "My Plugin",
        version = "1.0.0",
    )

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

### Step 2 — Editor plugin (canvas card + inspector panel)

```kotlin
object MyEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "com.example.my.editor",
        displayName = "My Editor Plugin",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        // Custom canvas card using ShapeCardFactory from graphyn-ui-cards
        // (circle, rounded square, or any Compose Shape)
        registrar.registerCanvasCard(
            "my.transform",
            ShapeCardFactory(
                shape = CircleShape,
                theme = ShapeNodeTheme(
                    background     = { Color(0xFF6366F1) },
                    selectedBorder = { Color(0xFF8B5CF6) },
                ),
                // Optional: replace initials with a custom avatar composable
                avatar = { node, spec ->
                    AsyncImage(
                        url = (node.config["avatar_url"] as? WorkflowValue.StringValue)?.value,
                    )
                }
            )
        )

        // Custom inspector panel with editable node config
        registrar.registerPanel("my.transform", EditorPanelFactory { ctx ->
            val input = (ctx.selectedNode?.config?.get("input") as? WorkflowValue.StringValue)?.value ?: ""
            BasicTextField(
                value = input,
                onValueChange = { ctx.onConfigChange("input", WorkflowValue.StringValue(it)) },
            )
        })

        // Node category (groups nodes in the palette)
        registrar.registerCategory(
            "com.example.text",
            NodeCategoryMeta(label = "Text", color = 0xFF6366F1L),
        )
    }
}
```

`ShapeCardFactory` accepts any Compose `Shape` — `CircleShape`, `RoundedCornerShape(12.dp)`, etc. Theme colors use `@Composable` lambdas so they can read from any `CompositionLocal` at render time. If no `avatar` is provided, the card shows the first letter of the node label.

#### FieldCard — supported input types

`FieldCardFactory` renders each input port as an inline editable row. The widget depends on the port's `WorkflowType`:

| `WorkflowType` | Widget | Interaction |
|---|---|---|
| `IntType` | Stepper | `−` / `+` buttons step by 1; click value to type; only digits and `−` accepted |
| `DoubleType` | Stepper | `−` / `+` buttons step by 0.1; click value to type; digits, `.`, `−` accepted |
| `StringType` | Text field | Click to edit inline; any text accepted |
| `BooleanType` | Text field | Click to type `true` or `false` |
| `EnumType(values)` | Single-select dropdown | Click chip → popup list; one option selected |
| `MultiEnumType(values)` | Multi-select dropdown | Click chip → popup with checkboxes; multiple options |

Non-editable port types (`OpaqueType`, `RecordType`, etc.) show the port label only — no input widget is rendered.

```kotlin
val myNode = NodeSpec(
    type = "com.example.sampler",
    label = "Sampler",
    inputs = listOf(
        PortSpec("steps",    WorkflowType.IntType),
        PortSpec("cfg",      WorkflowType.DoubleType),
        PortSpec("prompt",   WorkflowType.StringType),
        PortSpec("mode",     WorkflowType.EnumType(listOf("fast", "quality", "balanced"))),
        PortSpec("outputs",  WorkflowType.MultiEnumType(listOf("image", "latent", "preview"))),
    ),
    defaultValues = mapOf(
        "steps"   to WorkflowValue.IntValue(20),
        "cfg"     to WorkflowValue.DoubleValue(7.0),
        "prompt"  to WorkflowValue.StringValue(""),
        "mode"    to WorkflowValue.StringValue("quality"),
        "outputs" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue("image"))),
    ),
)
```

### Step 3 — Wire into the editor shell

```kotlin
@OptIn(GraphynExperimentalApi::class)
@Composable
fun App() {
    val runtimeRegistry = remember {
        DefaultGraphynPluginRegistry().apply { install(MyPlugin) }
    }
    val editorRegistry = remember {
        DefaultGraphynEditorPluginRegistry().apply { install(MyEditorPlugin) }
    }
    val state = rememberGraphynEditorState()

    // Observe workflow changes outside Compose (e.g. save to database)
    LaunchedEffect(state) {
        state.workflowFlow.collect { workflow ->
            database.save(workflow)
        }
    }

    GraphynEditorShell(
        state = state,
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs      = runtimeRegistry.nodeSpecs,
            canvasCards    = editorRegistry.canvasCards,
            panels         = editorRegistry.panels,
            categoryRegistry = editorRegistry.categories,
        ),
        branding = GraphynBranding(appName = "My Studio"),
    )
}
```

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

## Publishing

Artifacts are published to Maven Central automatically when a version tag is pushed (`git tag vX.Y.Z && git push origin vX.Y.Z`). For local / manual publishing without CI, use the Doppler-backed script:

```bash
# Prerequisites: doppler CLI + DOPPLER_TOKEN in .env or .env.local
# Doppler project "maven-central / prd" must have MAVEN_CENTRAL_USERNAME,
# MAVEN_CENTRAL_PASSWORD, GPG_SIGNING_KEY, GPG_SIGNING_PASSWORD

# Publish all modules at version from gradle.properties
./scripts/publish-local.sh

# Explicit version
./scripts/publish-local.sh 0.6.0

# Single module only (fastest for hotfixes)
./scripts/publish-local.sh 0.6.0 server
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
