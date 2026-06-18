# Graphyn

**Kotlin Multiplatform workflow editor library** — build visual node-graph editors for Android, Desktop, Web, iOS, and Server using a single shared codebase.

## Highlights

- Canvas with pan, zoom, and drag — powered by Compose Multiplatform
- MVI state management via `dispatch(GraphynEditorIntent)`
- Plugin-based node and panel registry
- Connection type validation with `WorkflowTypeCompatibility`
- Undo / redo, multi-select, copy / paste, palette search
- Node execution state badges (Running, Success, Error)
- Maven Central artifact + iOS XCFramework

## Artifacts

```kotlin
// Core domain model (no Compose)
implementation("io.github.ronjunevaldoz:graphyn-core:0.1.0")

// Plugin contracts
implementation("io.github.ronjunevaldoz:graphyn-plugin-api:0.1.0")

// Editor plugin contracts
implementation("io.github.ronjunevaldoz:graphyn-editor-api:0.1.0")

// Full Compose editor canvas
implementation("io.github.ronjunevaldoz:graphyn-editor:0.1.0")
```

## Quick example

```kotlin
val state = rememberGraphynEditorState(
    initialWorkflow = myWorkflow,
    nodeSpecs = myRegistry,
)

GraphynEditorShell(
    dependencies = GraphynEditorShellDependencies(nodeSpecs = myRegistry),
    state = state,
)
```

See [Quickstart](getting-started/quickstart.md) for a full walkthrough.
