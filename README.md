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
  <img src="https://img.shields.io/badge/pre--release-0.1.0-orange" alt="Pre-release"/>
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

| | |
|---|---|
| Pan & zoom canvas with minimap | |
| Connect nodes via port drag-and-drop | |
| Plugin system for runtime nodes and editor cards | |
| Inspector panel with per-node custom UI + write-back | |
| Built-in light / dark mode + theme presets | |
| Workflow validation with typed errors | |
| Workflow execution engine | |
| Observable workflow state (`StateFlow`) | |
| Auto-layout (topological sort, Cmd+Shift+L) | |
| Screenshot tests via Roborazzi | |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              app/shared (Compose UI)        │
│   canvas · shell · state · design system   │
└────────────┬────────────────┬───────────────┘
             │                │
     ┌───────▼──────┐  ┌──────▼───────┐
     │  editor-api  │  │  plugin-api  │
     │ panel slots  │  │ node specs   │
     │ canvas cards │  │ executors    │
     └───────┬──────┘  └──────┬───────┘
             │                │
         ┌───▼────────────────▼───┐
         │          core          │
         │  model · types · exec  │
         │  validation · registry │
         └────────────────────────┘

ui/cards sits between editor-api and app/shared:
implements NodeCanvasFactory with ready-to-use card shapes.
```

| Module | Artifact | Responsibility |
|---|---|---|
| `core` | `graphyn-core` | Workflow model, types, validation, execution engine — no Compose |
| `plugin-api` | `graphyn-plugin-api` | `GraphynPlugin` contract — register node specs and executors |
| `editor-api` | `graphyn-editor-api` | `GraphynEditorPlugin` contract — register canvas cards and inspector panels |
| `ui/cards` | `graphyn-ui-cards` | Reusable card factories — `ShapeCardFactory`, `FieldCardFactory` |
| `app/shared` | `graphyn-editor` | Compose Multiplatform canvas and editor shell |
| `plugins/sample-math` | — | Sample: math runtime plugin |
| `plugins/sample-logger` | — | Sample: logger runtime + editor plugin |
| `plugins/sample-style-nodes` | — | Sample: DarkHeaderCard demo, uses `ui/cards` factories |
| `server` | — | Sample: JVM server runtime wiring |
| `app/demo` | — | Sample: full editor app wiring |

---

## Installation

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

```kotlin
// gradle/libs.versions.toml
[versions]
graphyn = "0.1.0"

[libraries]
graphyn-editor     = { module = "io.github.ronjunevaldoz:graphyn-editor",     version.ref = "graphyn" }
graphyn-editor-api = { module = "io.github.ronjunevaldoz:graphyn-editor-api", version.ref = "graphyn" }
graphyn-plugin-api = { module = "io.github.ronjunevaldoz:graphyn-plugin-api", version.ref = "graphyn" }
graphyn-core       = { module = "io.github.ronjunevaldoz:graphyn-core",        version.ref = "graphyn" }
graphyn-ui-cards   = { module = "io.github.ronjunevaldoz:graphyn-ui-cards",   version.ref = "graphyn" }
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.graphyn.editor)      // full canvas — includes core, editor-api, plugin-api
    implementation(libs.graphyn.ui.cards)    // ShapeCardFactory + FieldCardFactory (optional)
    implementation(libs.graphyn.plugin.api)  // runtime plugin contract only (no Compose)
    implementation(libs.graphyn.editor.api)  // editor plugin contract only
    implementation(libs.graphyn.core)        // workflow model only (pure Kotlin)
}
```

| You want to… | Add |
|---|---|
| Embed the full canvas | `graphyn-editor` |
| Use ready-made card shapes | `graphyn-ui-cards` |
| Write a runtime plugin | `graphyn-plugin-api` |
| Write an editor plugin (custom card UI) | `graphyn-editor-api` |
| Use only the workflow model/types | `graphyn-core` |

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

## Testing

```bash
./gradlew :app:shared:jvmTest   # canvas + editor UI tests (Roborazzi)
./gradlew :core:check           # model, validation, and execution tests
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
