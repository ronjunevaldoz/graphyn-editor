# Custom Cards

By default every node renders as a `FieldCard` — a labelled panel with editable rows for each port. Implement `NodeCanvasFactory` to give a node type a completely custom appearance.

---

## The factory contract

```kotlin
interface NodeCanvasFactory {
    @Composable
    fun NodeCanvas(context: NodeCanvasContext)

    val nodeWidth: Int   get() = 280   // dp
    val nodeHeight: Int  get() = 180   // dp
    val nodeShape: NodeShape get() = NodeShape.Rectangle

    fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int
}
```

The editor uses `nodeWidth`, `nodeHeight`, and `portAnchorY` to draw connection wires and the minimap. Override all three to match your card's actual layout — mismatched values result in wires that miss their ports.

---

## NodeCanvasContext

Your composable receives a `NodeCanvasContext` at render time:

```kotlin
data class NodeCanvasContext(
    val node: NodeRef,                  // current node (id, type, config)
    val spec: NodeSpec,                 // spec for this node type
    val selected: Boolean,              // whether the node is selected
    val executionStatus: NodeExecutionStatus,
    val onSelect: () -> Unit,           // call to select this node
    val onMove: (IntOffset) -> Unit,    // call to move, delta in dp
    val onConfigChange: (key: String, value: WorkflowValue) -> Unit,
    val contentColor: Color,            // canvas surface text colour
)
```

`onConfigChange` is the only way to persist config edits. The editor dispatches `GraphynEditorIntent.UpdateNodeConfig` in response, which feeds into undo/redo.

---

## Minimal example — circle trigger card

```kotlin
class TriggerCardFactory : NodeCanvasFactory {

    override val nodeWidth  = 64
    override val nodeHeight = 64
    override val nodeShape  = NodeShape.Circle

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec) =
        nodeHeight / 2  // single port centred vertically

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        val border = if (context.selected) Color(0xFF7AA2FF) else Color(0xFF3D3E52)

        Box(
            modifier = Modifier
                .size(nodeWidth.dp, nodeHeight.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D2E3D))
                .border(2.dp, border, CircleShape)
                .clickable { context.onSelect() }
                .graphynDragGesture(context.onMove),   // helper from editor-api
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                context.spec.label,
                style = TextStyle(color = Color(0xFFE0E0E6), fontSize = 10.sp),
            )
        }
    }
}
```

---

## Drag gesture

The `graphynDragGesture` modifier (from `editor-api`) translates pointer events to `IntOffset` deltas in dp and calls `context.onMove`. Use it on your card's drag handle:

```kotlin
Modifier.graphynDragGesture(context.onMove)
```

If your card has a header and a body, apply the gesture only to the header so the body can receive its own click/scroll events:

```kotlin
// Header — draggable
Box(Modifier.graphynDragGesture(context.onMove)) { ... }

// Body — receives its own events
Box(Modifier.clickable { /* something */ }) { ... }
```

---

## Reading and writing config

Read config values from `context.node.config`:

```kotlin
val text = (context.node.config["text"] as? WorkflowValue.StringValue)?.value ?: ""
```

Write back via `context.onConfigChange`:

```kotlin
BasicTextField(
    value = text,
    onValueChange = { context.onConfigChange("text", WorkflowValue.StringValue(it)) },
)
```

Config changes are debounced by the editor's intent dispatch — you don't need to debounce them yourself.

---

## Registering the factory

In your `GraphynEditorPlugin`:

```kotlin
override fun register(registrar: GraphynEditorPluginRegistrar) {
    registrar.registerCanvasCard("myplugin.trigger", TriggerCardFactory())
}
```

---

## Port anchor alignment

The editor draws connection wires from `(nodeX + nodeWidth, portAnchorY)` for outputs and `(nodeX, portAnchorY)` for inputs. If your card draws port dots at custom positions, override `portAnchorY` to match:

```kotlin
override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int {
    val headerHeight = 24
    val rowHeight    = 32
    val rowGap       = 8
    return headerHeight + rowHeight / 2 + portIndex * (rowHeight + rowGap)
}
```

Port index is zero-based, ordered top-to-bottom as declared in the `NodeSpec`.

---

## Execution status badge

Show `NodeStatusBadge` from `editor-api` on your card to display Running / Success / Error state:

```kotlin
Box {
    // ... your card content
    NodeStatusBadge(
        status = context.executionStatus,
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
    )
}
```

See `NodeStatusBadge` KDoc for badge appearance and placement guidance.
