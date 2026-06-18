# graphyn-editor

Compose Multiplatform workflow editor canvas — the main publishable UI module.

## Dependency

```kotlin
implementation("io.github.ronjunevaldoz:graphyn-editor:0.1.0")
```

Transitively provides `graphyn-core`, `graphyn-plugin-api`, and `graphyn-editor-api`.

## Embedding the editor

```kotlin
@Composable
fun MyApp() {
    val registry = remember {
        DefaultGraphynPluginRegistry().apply { install(MyPlugin) }
    }
    App(
        plugins = listOf(MyPlugin),
        executionEngine = WorkflowExecutionEngine(registry.nodeExecutors, registry.nodeSpecs),
    )
}
```

## Key entry points

| Symbol | Purpose |
|---|---|
| `GraphynEditorShell` | Root composable — canvas + palette + inspector + toolbar |
| `GraphynEditorShellDependencies` | Wires node specs, panels, canvas cards, execution engine |
| `GraphynEditorState` / `rememberGraphynEditorState` | All canvas state; mutations via `dispatch(GraphynEditorIntent)` |
| `GraphynTheme` | Applies design tokens; wrap the shell with this |
| `GraphynBranding` | App name, logo, palette override |

## Targets

Android · Desktop (JVM) · Web (Wasm) · Web (JS) · iOS (via XCFramework)
