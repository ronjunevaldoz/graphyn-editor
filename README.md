# Graphyn

[![CI](https://github.com/ronjunevaldoz/graphyn2/actions/workflows/ci.yml/badge.svg)](https://github.com/ronjunevaldoz/graphyn2/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-3DDC84)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![KMP](https://img.shields.io/badge/Kotlin-Multiplatform-0095D5)](https://kotlinlang.org/docs/multiplatform.html)

Graphyn is a Kotlin Multiplatform workflow editor library. Drop it into any app that needs a node-based canvas — AI pipelines, shader graphs, game editor tools, automation builders.

- `core` — workflow model, types, validation, execution
- `plugin-api` — contract for runtime node plugins
- `editor-api` — contract for editor panel plugins
- `app/shared` — Compose Multiplatform canvas and shell UI

## Quick Start

> Graphyn is pre-1.0. Clone the repo and use local Maven until the first release is tagged.

```bash
git clone https://github.com/ronjunevaldoz/graphyn2.git
cd graphyn2
./gradlew publishToMavenLocal
```

### 1. Implement a runtime plugin

A runtime plugin registers node specs (what the node looks like) and executors (what it does).

```kotlin
// Define node specs
object MathNodes {
    val add = NodeSpec(
        type = "math.add",
        label = "Add",
        inputs = listOf(
            PortSpec(name = "left",  type = WorkflowType.DoubleType, required = false),
            PortSpec(name = "right", type = WorkflowType.DoubleType, required = false),
        ),
        outputs = listOf(PortSpec(name = "result", type = WorkflowType.DoubleType)),
        defaultValues = mapOf(
            "left"  to WorkflowValue.DoubleValue(0.0),
            "right" to WorkflowValue.DoubleValue(0.0),
        ),
    )
}

// Implement the plugin
object MathPlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "com.example.math",
        displayName = "Math",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(MathNodes.add)
        registrar.registerExecutor("math.add") { inputs ->
            val left  = (inputs["left"]  as? WorkflowValue.DoubleValue)?.value ?: 0.0
            val right = (inputs["right"] as? WorkflowValue.DoubleValue)?.value ?: 0.0
            mapOf("result" to WorkflowValue.DoubleValue(left + right))
        }
    }
}
```

### 2. Implement an editor panel plugin (optional)

An editor panel plugin adds a custom inspector panel for a node type.

```kotlin
object MathEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "com.example.math.editor",
        displayName = "Math Editor",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerPanel("math.add", EditorPanelFactory { context ->
            // Composable panel shown in the inspector when an Add node is selected
            BasicText("Left: ${context.selectedNodeOutputs["left"]}")
        })
    }
}
```

### 3. Wire into the editor shell

```kotlin
val runtimeRegistry = DefaultGraphynPluginRegistry().apply {
    install(MathPlugin)
}
val editorRegistry = DefaultGraphynEditorPluginRegistry().apply {
    install(MathEditorPlugin)
}
val engine = WorkflowExecutionEngine(
    executors = runtimeRegistry.nodeExecutors,
    specs = runtimeRegistry.nodeSpecs,
)

@Composable
fun App() {
    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs = runtimeRegistry.nodeSpecs,
            panels = editorRegistry.panels,
            executionEngine = engine,
        ),
        branding = GraphynBranding(appName = "My Studio"),
    )
}
```

A working reference implementation is in [`plugins/sample-math`](./plugins/sample-math).

## Plugin Model

Graphyn separates runtime concerns from editor concerns:

| Layer | Module | Implements |
|---|---|---|
| Runtime | `plugin-api` | `GraphynPlugin` — node specs + executors |
| Editor | `editor-api` | `GraphynEditorPlugin` — inspector panels |

The two registries are independent. You can use a runtime plugin without an editor plugin (nodes appear with the default inspector).

## Layout

- [`/core`](./core/src) — workflow model, validation, execution
- [`/plugin-api`](./plugin-api/src) — runtime plugin contract
- [`/editor-api`](./editor-api/src) — editor plugin contract
- [`/app/shared`](./app/shared/src) — Compose Multiplatform canvas and shell
- [`/app/desktopApp`](./app/desktopApp) — Desktop entrypoint
- [`/app/androidApp`](./app/androidApp) — Android entrypoint
- [`/app/webApp`](./app/webApp) — Web entrypoint
- [`/plugins`](./plugins) — first-party sample plugins
- [`/docs`](./docs) — architecture notes and plans

## Running

- Desktop: `./gradlew :app:desktopApp:run`
- Android: `./gradlew :app:androidApp:assembleDebug`
- Web (Wasm): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
- Web (JS): `./gradlew :app:webApp:jsBrowserDevelopmentRun`

## Testing

```bash
./gradlew :app:shared:jvmTest   # canvas + editor tests
./gradlew :core:check           # model and validation tests
```

## Docs

- [Architecture overview](./docs/architecture/README.md)
- [Plugin API](./docs/architecture/plugins.md)
- [Engineering lessons](./docs/architecture/lessons.md)
- [Plan phases](./docs/plans/README.md)
