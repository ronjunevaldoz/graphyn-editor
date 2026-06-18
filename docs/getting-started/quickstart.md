# Quickstart

## 1. Define your node specs

```kotlin
val registry = DefaultNodeSpecRegistry().apply {
    register(
        NodeSpec(
            type = "add",
            label = "Add",
            inputs = listOf(PortSpec("a", WorkflowType.NumberType), PortSpec("b", WorkflowType.NumberType)),
            outputs = listOf(PortSpec("result", WorkflowType.NumberType)),
        )
    )
}
```

## 2. Create a workflow

```kotlin
val workflow = WorkflowDefinition(
    id = "demo",
    name = "Demo Workflow",
    nodes = listOf(NodeRef("n1", "add"), NodeRef("n2", "add")),
    connections = listOf(ConnectionRef("n1", "result", "n2", "a")),
)
```

## 3. Show the editor

```kotlin
@Composable
fun App() {
    val state = rememberGraphynEditorState(
        initialWorkflow = workflow,
        nodeSpecs = registry,
    )

    GraphynEditorShell(
        dependencies = GraphynEditorShellDependencies(
            nodeSpecs = registry,
            executionEngine = WorkflowExecutionEngine(myExecutorRegistry),
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
val json = workflow.toJson()                    // → JSON string
val loaded = workflowFromJson(json)             // → WorkflowDefinition
```
