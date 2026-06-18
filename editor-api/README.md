# graphyn-editor-api

Contract for Graphyn editor plugins — register custom node card UI and inspector panels.

## Dependency

```kotlin
implementation("io.github.ronjunevaldoz:graphyn-editor-api:0.1.0")
```

## Writing an editor plugin

```kotlin
object MathEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "com.example.math.editor",
        displayName = "Math Editor",
        version = "1.0.0",
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        // Custom inspector panel for a node type
        registrar.registerPanel("math.add", EditorPanelFactory { context ->
            BasicText("Result: ${context.selectedNodeOutputs["result"]}")
        })

        // Custom canvas card for a node type
        registrar.registerCanvasCard("math.add", NodeCanvasFactory { context ->
            MyMathCard(context)
        })
    }
}
```

## Components available to card authors

- `NodeStatusBadge(status, modifier, surfaceColor)` — execution status indicator (Idle/Running/Success/Error)

Requires Compose Foundation — does not depend on Compose Material.
