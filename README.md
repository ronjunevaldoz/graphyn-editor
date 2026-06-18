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
  <img src="https://img.shields.io/badge/pre--release-local%20maven-orange" alt="Pre-release"/>
</p>

---

## What is Graphyn?

Graphyn is a **Kotlin Multiplatform library** that gives your app a fully-featured node-based canvas — the kind used in Unreal Blueprint, Blender shader editor, or n8n. Wire it up with plugins to define your own node types, connect them, and execute workflows.

- **Library-first** — embed the canvas in your own app, not the other way around
- **Plugin architecture** — runtime plugins define node specs and executors; editor plugins add custom inspector panels
- **Compose Multiplatform UI** — one codebase for Android, Desktop (JVM), Web (Wasm/JS), and iOS
- **MVI state model** — all canvas mutations go through typed intents; easy to test and replay
- **Zero Material dependency** — custom design system, fully themeable

---

## Features

| | |
|---|---|
| 🎨 | Pan & zoom canvas with minimap |
| 🔌 | Connect nodes via port drag-and-drop |
| 🧩 | Plugin system for runtime nodes and editor panels |
| 🎛️ | Inspector panel with per-node custom UI |
| 🌗 | Built-in light / dark mode + theme presets |
| ✅ | Workflow validation with typed errors |
| ⚡ | Workflow execution engine |
| 🧪 | Screenshot tests via Roborazzi |

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
     │ editor plugs │  │ executors    │
     └───────┬──────┘  └──────┬───────┘
             │                │
         ┌───▼────────────────▼───┐
         │          core          │
         │  model · types · exec  │
         │  validation · registry │
         └────────────────────────┘
```

| Module | Responsibility |
|---|---|
| `core` | Workflow model, types, validation, execution engine — no Compose |
| `plugin-api` | `GraphynPlugin` contract — register node specs and executors |
| `editor-api` | `GraphynEditorPlugin` contract — register inspector panel UI |
| `app/shared` | Compose Multiplatform canvas and editor shell |
| `server` | JVM runtime host |
| `plugins/sample-math` | Reference math plugin |
| `plugins/sample-logger` | Reference logger plugin |

---

## Quick Start

> **Pre-1.0:** clone the repo and publish to local Maven until the first release is tagged.

```bash
git clone https://github.com/ronjunevaldoz/graphyn-editor.git
cd graphyn-editor
./gradlew publishToMavenLocal
```

### Step 1 — Implement a runtime plugin

A runtime plugin registers node specs (what nodes look like and their ports) and executors (what they do).

```kotlin
object MathPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "com.example.math",
        displayName = "Math",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(
            NodeSpec(
                type = "math.add",
                label = "Add",
                inputs = listOf(
                    PortSpec(name = "left",  type = WorkflowType.DoubleType),
                    PortSpec(name = "right", type = WorkflowType.DoubleType),
                ),
                outputs = listOf(
                    PortSpec(name = "result", type = WorkflowType.DoubleType),
                ),
                defaultValues = mapOf(
                    "left"  to WorkflowValue.DoubleValue(0.0),
                    "right" to WorkflowValue.DoubleValue(0.0),
                ),
            )
        )
        registrar.registerExecutor("math.add") { inputs ->
            val left  = (inputs["left"]  as? WorkflowValue.DoubleValue)?.value ?: 0.0
            val right = (inputs["right"] as? WorkflowValue.DoubleValue)?.value ?: 0.0
            mapOf("result" to WorkflowValue.DoubleValue(left + right))
        }
    }
}
```

### Step 2 — Implement an editor panel plugin (optional)

An editor panel plugin adds a custom inspector UI for a node type.

```kotlin
object MathEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "com.example.math.editor",
        displayName = "Math Editor",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerPanel("math.add", EditorPanelFactory { context ->
            BasicText("Result: ${context.selectedNodeOutputs["result"]}")
        })
    }
}
```

### Step 3 — Wire into the editor shell

```kotlin
@OptIn(GraphynExperimentalApi::class)
val runtimeRegistry = DefaultGraphynPluginRegistry().apply {
    install(MathPlugin)
}

@Composable
fun App() {
    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs = runtimeRegistry.nodeSpecs,
        ),
        branding = GraphynBranding(appName = "My Studio"),
    )
}
```

A full working reference is in [`plugins/sample-math`](./plugins/sample-math).

---

## Plugin Model

Graphyn separates runtime concerns from editor concerns — you can ship a runtime plugin without any UI dependency.

| Layer | Module | Stable contract |
|---|---|---|
| Runtime | `plugin-api` | `GraphynPlugin`, `GraphynPluginRegistrar`, `GraphynPluginRegistry` |
| Editor | `editor-api` | `GraphynEditorPlugin`, `GraphynEditorPluginRegistrar`, `EditorPanelFactory` |

`Default*` implementations (`DefaultGraphynPluginRegistry`, `DefaultEditorPanelRegistry`, etc.) are marked `@GraphynExperimentalApi` — their signatures may evolve. The interfaces above are the stable contract.

---

## Platform Targets

| Platform | Status |
|---|---|
| Android | ✅ |
| Desktop (JVM) | ✅ |
| Web (Wasm) | ✅ |
| Web (JS) | ✅ |
| iOS | 🔜 |
| Server (JVM) | ✅ runtime only |

---

## Running

```bash
# Desktop
./gradlew :app:desktopApp:run

# Android
./gradlew :app:androidApp:assembleDebug

# Web (Wasm)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# Web (JS)
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

---

## Testing

```bash
./gradlew :app:shared:jvmTest   # canvas + editor UI tests (Roborazzi)
./gradlew :core:check           # model, validation, and execution tests
```

---

## Docs

- [Architecture overview](./docs/architecture/README.md)
- [Plugin API](./docs/architecture/plugins.md)
- [Core API](./docs/architecture/core-api.md)
- [Engineering lessons](./docs/architecture/lessons.md)
- [Test coverage](./docs/architecture/test-coverage.md)

---

## License

MIT © [Ron June Valdoz](https://github.com/ronjunevaldoz)
