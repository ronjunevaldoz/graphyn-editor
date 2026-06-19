# Read-Only Mode

Graphyn does not have a built-in read-only flag, but you can achieve it at two levels depending on how much control you need.

---

## Level 1 — Intercept intent dispatch

The simplest approach: wrap `GraphynEditorState` and ignore all mutation intents. All canvas interactions (drag, connect, delete, undo) go through `dispatch(GraphynEditorIntent)`. Suppress the dispatch to make the canvas non-destructive.

```kotlin
class ReadOnlyEditorState(
    workflow: WorkflowDefinition,
) : GraphynEditorState(initialWorkflow = workflow) {

    override fun dispatch(intent: GraphynEditorIntent) {
        // Allow viewport intents (pan, zoom) so the user can navigate
        if (intent is GraphynEditorIntent.Pan ||
            intent is GraphynEditorIntent.Zoom ||
            intent is GraphynEditorIntent.FitToScreen ||
            intent is GraphynEditorIntent.SelectNode ||
            intent is GraphynEditorIntent.DeselectAll
        ) {
            super.dispatch(intent)
        }
        // All structural intents (AddNode, DeleteNode, AddConnection, etc.) are silently dropped
    }
}
```

Then use it in place of `rememberGraphynEditorState`:

```kotlin
val state = remember(workflow) { ReadOnlyEditorState(workflow) }

GraphynEditorShell(
    dependencies = GraphynEditorShellDependencies(nodeSpecs = mySpecs),
    state = state,
)
```

---

## Level 2 — Hide editing controls

The shell renders a toolbar with a run button, palette, and inspector. If the viewer should not see those, pass a custom `canvas` slot and skip the shell entirely:

```kotlin
GraphynTheme {
    GraphynCanvasSurface(
        state = state,
        nodeSpecs = mySpecs,
        modifier = Modifier.fillMaxSize(),
    )
}
```

`GraphynCanvasSurface` is the raw canvas without any panels or toolbar. Combined with the `ReadOnlyEditorState` above, this gives a clean view-only embed.

---

## Level 3 — Conditional editing by role

For role-based access (some users edit, others view), pass a flag into your composable:

```kotlin
@Composable
fun WorkflowScreen(workflow: WorkflowDefinition, canEdit: Boolean) {
    val state = remember(workflow, canEdit) {
        if (canEdit) GraphynEditorState(initialWorkflow = workflow)
        else ReadOnlyEditorState(workflow)
    }

    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(nodeSpecs = mySpecs),
        state = state,
    )
}
```

> **Note:** A first-class `readOnly` parameter on `GraphynEditorShell` is planned for a future release. Until then, the dispatch-intercept approach above is the recommended pattern.
