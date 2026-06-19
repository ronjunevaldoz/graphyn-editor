# Analytics Hooks

Graphyn does not ship an analytics SDK, but the `GraphynEditorIntent` dispatch path is the natural instrumentation point — every meaningful user action passes through it.

---

## Intercept intent dispatch

Subclass `GraphynEditorState` and override `dispatch` to track any intent before it executes:

```kotlin
class TrackedEditorState(
    initialWorkflow: WorkflowDefinition?,
    private val analytics: AnalyticsClient,
) : GraphynEditorState(initialWorkflow = initialWorkflow) {

    override fun dispatch(intent: GraphynEditorIntent) {
        track(intent)
        super.dispatch(intent)
    }

    private fun track(intent: GraphynEditorIntent) {
        when (intent) {
            is GraphynEditorIntent.AddNode ->
                analytics.track("node_added", mapOf("type" to intent.spec.type))

            is GraphynEditorIntent.DeleteSelected ->
                analytics.track("node_deleted")

            is GraphynEditorIntent.CompleteConnection ->
                analytics.track("connection_created")

            is GraphynEditorIntent.RunWorkflow ->
                analytics.track("workflow_run")

            is GraphynEditorIntent.CreateGroupFromSelection ->
                analytics.track("group_created")

            else -> Unit  // pan, zoom, select — usually too noisy to track
        }
    }
}
```

Use it in place of `rememberGraphynEditorState`:

```kotlin
val state = remember(workflow) {
    TrackedEditorState(initialWorkflow = workflow, analytics = myAnalyticsClient)
}
```

---

## Recommended events

| Event | Intent | Useful properties |
|---|---|---|
| `node_added` | `AddNode` | `spec.type`, `spec.category` |
| `node_deleted` | `DeleteSelected` | — |
| `connection_created` | `CompleteConnection` | from/to port types |
| `connection_deleted` | `DeleteSelected` (on connection) | — |
| `workflow_run` | `RunWorkflow` | node count |
| `workflow_saved` | — (your persistence layer) | workflow id |
| `undo` | `Undo` | — |
| `redo` | `Redo` | — |
| `palette_searched` | `SetPaletteSearch` | query length (not content) |
| `group_created` | `CreateGroupFromSelection` | node count in group |

---

## Session timing

Track time-on-editor by noting when the composable enters and leaves composition:

```kotlin
DisposableEffect(state) {
    val sessionStart = Clock.System.now()
    onDispose {
        val duration = Clock.System.now() - sessionStart
        analytics.track("editor_session_ended", mapOf("duration_ms" to duration.inWholeMilliseconds))
    }
}
```

---

## Privacy note

- Never log raw config values (they may contain user credentials or PII).
- Do not log workflow names or node labels without consent — they may contain business-sensitive information.
- Log node types and structural metadata only.
