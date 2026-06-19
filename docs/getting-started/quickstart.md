# Quickstart

## 1. Define your node specs and executors

```kotlin
val specs = DefaultNodeSpecRegistry().apply {
    register(
        NodeSpec(
            type = "add",
            label = "Add",
            inputs  = listOf(PortSpec("a", WorkflowType.NumberType), PortSpec("b", WorkflowType.NumberType)),
            outputs = listOf(PortSpec("result", WorkflowType.NumberType)),
        )
    )
}

val executors = DefaultNodeExecutorRegistry().apply {
    register("add") { inputs ->
        val a = (inputs["a"] as? WorkflowValue.NumberValue)?.value ?: 0.0
        val b = (inputs["b"] as? WorkflowValue.NumberValue)?.value ?: 0.0
        mapOf("result" to WorkflowValue.NumberValue(a + b))
    }
}
```

## 2. Create an initial workflow

```kotlin
val workflow = WorkflowDefinition(
    id    = "my-workflow",
    name  = "My Workflow",
    nodes = listOf(NodeRef("n1", "add"), NodeRef("n2", "add")),
    connections = listOf(ConnectionRef("n1", "result", "n2", "a")),
)
```

## 3. Show the editor

```kotlin
@Composable
fun App() {
    val state = rememberGraphynEditorState(initialWorkflow = workflow)

    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs       = specs,
            executionEngine = WorkflowExecutionEngine(executors, specs),
        ),
        state = state,
    )
}
```

## Keyboard shortcuts

| Shortcut | Action |
|---|---|
| `Cmd/Ctrl + Z` | Undo |
| `Cmd/Ctrl + Shift + Z` | Redo |
| `Cmd/Ctrl + A` | Select all |
| `Cmd/Ctrl + C` | Copy selection |
| `Cmd/Ctrl + V` | Paste |
| `Cmd/Ctrl + D` | Duplicate |
| `Escape` | Cancel / deselect |
| `Backspace / Delete` | Delete selected |

## Serialization

```kotlin
val json   = workflow.toJson()       // WorkflowDefinition → JSON string
val loaded = workflowFromJson(json)  // JSON string → WorkflowDefinition
```

## What's next

- Use a plugin instead of manual registration — see [Plugin authoring](../guides/plugin-authoring.md)
- Persist workflows to disk or a backend — see [Persistence](../guides/persistence.md)
- Run the editor locally — see [Running locally](../guides/running-locally.md)
