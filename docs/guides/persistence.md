# Persistence

Graphyn holds workflow state in memory. Saving and loading is your responsibility — the library deliberately has no opinion on where workflows live (a database, a REST API, local files). This guide shows three common patterns.

---

## Reading current workflow state

`GraphynEditorState.workflow` is a `@Stable` observable property. Read it at any time:

```kotlin
val state = rememberGraphynEditorState(initialWorkflow = myWorkflow)

// Read the current workflow (reflects all edits, undo/redo, etc.)
val current: WorkflowDefinition? = state.workflow
```

---

## Pattern 1 — Auto-save with debounce

Observe `state.workflow` inside a `LaunchedEffect` and debounce writes to avoid saving on every keystroke.

```kotlin
@Composable
fun MyEditor(repo: WorkflowRepository) {
    val state = rememberGraphynEditorState(initialWorkflow = repo.load())

    // Auto-save: wait 1 s of inactivity after any change, then save
    val workflow = state.workflow
    LaunchedEffect(workflow) {
        if (workflow != null) {
            delay(1_000)
            repo.save(workflow)
        }
    }

    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(nodeSpecs = repo.specs),
        state = state,
    )
}
```

`LaunchedEffect(workflow)` restarts the coroutine every time `workflow` changes. The `delay(1_000)` is cancelled and restarted on each change, so the save only fires after a 1-second pause.

---

## Pattern 2 — Explicit save button

Let the user control when saves happen. Store a `dirty` flag and expose a save action.

```kotlin
@Composable
fun MyEditor(repo: WorkflowRepository) {
    val state = rememberGraphynEditorState(initialWorkflow = repo.load())
    val initial = remember { state.workflow }
    val isDirty = state.workflow != initial

    Column {
        if (isDirty) {
            Button(onClick = {
                state.workflow?.let { repo.save(it) }
            }) { Text("Save") }
        }
        GraphynEditorShell(
            dependencies = GraphynEditorShellDependencies(nodeSpecs = repo.specs),
            state = state,
        )
    }
}
```

---

## Pattern 3 — Load from a REST API

Fetch the workflow from a backend on first composition and pass it to the editor.

```kotlin
@Composable
fun MyEditor(workflowId: String, api: WorkflowApi) {
    var workflow by remember { mutableStateOf<WorkflowDefinition?>(null) }

    LaunchedEffect(workflowId) {
        workflow = api.fetchWorkflow(workflowId) // suspend fun, returns WorkflowDefinition
    }

    // key() recreates editor state when the workflow identity changes
    key(workflowId) {
        val state = rememberGraphynEditorState(initialWorkflow = workflow)
        GraphynEditorShell(
            dependencies = GraphynEditorShellDependencies(nodeSpecs = mySpecs),
            state = state,
        )
    }
}
```

To save back:

```kotlin
suspend fun saveToApi(state: GraphynEditorState, api: WorkflowApi) {
    val wf = state.workflow ?: return
    api.updateWorkflow(wf.id, wf.toJson())
}
```

---

## Serialization

```kotlin
val json: String = workflow.toJson()                  // WorkflowDefinition → JSON string
val loaded: WorkflowDefinition = workflowFromJson(json) // JSON string → WorkflowDefinition
```

See [Serialization reference](../reference/serialization.md) for the full JSON schema.

---

## What is and isn't saved

`WorkflowDefinition` contains:

| Field | Saved |
|---|---|
| Node types, IDs, config values | ✅ |
| Connections between ports | ✅ |
| Workflow name and ID | ✅ |
| Canvas positions (node layout) | ❌ — viewport/layout state is editor-only |
| Group frames (Cmd+G) | ❌ — editor-only visual state |
| Undo/redo history | ❌ — session-only |

Node positions are not part of `WorkflowDefinition`. If you need to persist layout, read `state.layout.nodePositionsByNodeId` separately and restore them after loading.
