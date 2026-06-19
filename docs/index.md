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
implementation("io.github.ronjunevaldoz:graphyn-core:0.2.0")

// Plugin contracts
implementation("io.github.ronjunevaldoz:graphyn-plugin-api:0.2.0")

// Editor plugin contracts
implementation("io.github.ronjunevaldoz:graphyn-editor-api:0.2.0")

// Full Compose editor canvas
implementation("io.github.ronjunevaldoz:graphyn-editor:0.2.0")

// Optional — pre-built card styles
implementation("io.github.ronjunevaldoz:graphyn-ui-cards:0.2.0")

// Optional — HTTP / File IO executor nodes
implementation("io.github.ronjunevaldoz:graphyn-plugin-io:0.2.0")
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

---

## Getting started

- [Installation](getting-started/installation.md)
- [Quickstart](getting-started/quickstart.md)
- [Running locally](guides/running-locally.md)

---

## Guides

### Integration
- [Running locally — desktopApp vs sample vs demo](guides/running-locally.md)
- [Persistence — save and load workflows](guides/persistence.md)
- [Read-only mode](guides/read-only-mode.md)
- [Remote execution — server-side node runners](guides/remote-execution.md)
- [Embedding on Android](guides/embedding-android.md)
- [Embedding on Web (JS / WASM)](guides/embedding-web.md)

### Extending
- [Plugin authoring](guides/plugin-authoring.md)
- [Custom cards](guides/custom-cards.md)
- [Theming](guides/theming.md)
- [Plugin versioning](guides/plugin-versioning.md)

### Production
- [Analytics hooks](guides/analytics-hooks.md)

---

## Reference

- [Type system](reference/type-system.md)
- [Keyboard shortcuts](reference/keyboard-shortcuts.md)
- [Serialization](reference/serialization.md)

---

## Architecture

- [Roadmap](architecture/roadmap.md)
- [Test coverage](architecture/test-coverage.md)
- [Lessons learned](architecture/lessons.md)
