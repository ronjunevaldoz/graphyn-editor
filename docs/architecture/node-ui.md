# Node UI System

How node cards are rendered on the canvas, and how to build custom ones.

---

## Card shapes

Three pre-built card shapes ship in `plugins/sample-style-nodes`. Each maps to a `NodeCanvasFactory` registered through `GraphynEditorPlugin`.

### DarkHeaderCard

`DarkHeaderCard` — `plugins/sample-style-nodes/DarkHeaderCard.kt`

Multi-port card with a coloured header bar. Use for nodes with many inputs and outputs that benefit from port-level colour coding.

```
┌─────────────────────────────┐
│  Node Label         (header) │
├─────────────────────────────┤
│ ● inputA        outputX ●   │
│ ● inputB        outputY ●   │
└─────────────────────────────┘
```

- Width: 200 dp
- Port dot colour: `PortSpec.portColor` (falls back to muted grey)
- Header colour: `DARK_HEADER_BG` from `StyleNodeSharedColors`

### FieldCard

`FieldCard` — `ui/cards/FieldCardFactory.kt`

Labelled field rows. Use for nodes where inputs are configurable values the user edits directly on the canvas.

```
┌─────────────────────────────┐
│  Node Label                  │
├─────────────────────────────┤
│ count          [  7  ]       │
│ mode           [ fast ▾]     │
│ tags           [2 items ▾]   │
│ coords         {2 fields} ▾  │
├─────────────────────────────┤
│                     result   │
└─────────────────────────────┘
```

- Width: 220 dp
- Height: auto from `inputRows` + `outputRows`
- Output ports appear below the divider, right-aligned

#### Supported input types

| `WorkflowType` | Widget | Behaviour |
|---|---|---|
| `StringType` | Text chip | Click to inline-edit; commits on focus loss |
| `BooleanType` | Text chip (`true`/`false`) | Click to inline-edit |
| `IntType` | `−\|value\|+` stepper | Click center to type; `−`/`+` steps by 1 |
| `DoubleType` | `−\|value\|+` stepper | Click center to type; `−`/`+` steps by 0.1 |
| `EnumType(values)` | Dropdown chip | Single-select popup; width capped at 160 dp |
| `MultiEnumType(values)` | Dropdown chip | Multi-select popup with checkboxes |
| `ListType(elementType)` | `N items ▾` chip | Popup: add/remove items, inline edit per item |
| `RecordType(fields)` | `{ N fields } ▾` chip | Popup: one editable row per field key |
| `NullableType` | Falls through to inner type widget | — |
| `OpaqueType` | Text chip | Accepts any connected type; no editing |

#### Customising the theme

```kotlin
FieldCardFactory(
    theme = FieldNodeTheme(
        background      = { Color(0xFF1E1E1E) },
        headerBackground = { Color(0xFF2D2D2D) },
        border          = { Color(0xFF444444) },
        selectedBorder  = { Color(0xFF61AFEF) },
        labelColor      = { Color(0xFF9DA5B4) },
        valueBg         = { Color(0xFF282C34) },
        valueText       = { Color(0xFFABB2BF) },
        divider         = { Color(0xFF3E4452) },
    ),
    inputRows = 4,
    outputRows = 2,
)
```

### CircleCard (ShapeCard)

`CircleCard` — `ui/cards/ShapeCardFactory.kt`

Compact round trigger/sink node. Use for webhook sources, event emitters, or any node with zero or one port.

```
    ╭────╮
    │ ▶  │
    ╰────╯
```

- Diameter: 80 dp
- Single port anchor at card edge

---

## NodeCanvasFactory contract

Every canvas card is produced by a `NodeCanvasFactory`. Register one per node type through `GraphynEditorPluginRegistrar.registerCanvasCard`.

```kotlin
interface NodeCanvasFactory {
    val nodeWidth: Int   // dp
    val nodeHeight: Int  // dp

    @Composable
    fun NodeCanvas(context: NodeCanvasContext)

    fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int
}
```

`NodeCanvasContext` carries everything the card needs at render time:

```kotlin
data class NodeCanvasContext(
    val node: NodeRef,           // live node (id + config)
    val spec: NodeSpec,          // schema (ports, defaults)
    val selected: Boolean,
    val executionStatus: NodeExecutionStatus,
    val onSelect: () -> Unit,
    val onMove: (IntOffset) -> Unit,
    val onConfigChange: (key: String, value: WorkflowValue) -> Unit = { _, _ -> },
)
```

`portAnchorY` returns the Y offset (in dp) from the top of the card where a port's connection wire attaches. The canvas uses this to draw connection lines.

---

## Building a custom card

```kotlin
object MyCardFactory : NodeCanvasFactory {
    override val nodeWidth  = 160
    override val nodeHeight = 80

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) {
        Box(
            Modifier.width(nodeWidth.dp).height(nodeHeight.dp)
                .background(Color(0xFF1A1A2E))
                .border(1.dp, if (context.selected) Color.Cyan else Color.DarkGray, RoundedCornerShape(8.dp))
                .clickable { context.onSelect() }
        ) {
            BasicText(context.spec.label, style = TextStyle(color = Color.White))
        }
    }

    override fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec) = nodeHeight / 2
}

// Register in your editor plugin:
override fun register(registrar: GraphynEditorPluginRegistrar) {
    registrar.registerCanvasCard("my.node.type", MyCardFactory)
}
```

---

## Execution status badge

All three built-in cards display a `NodeStatusBadge` in the top-right corner when a node is running, succeeded, or failed. It is driven by `NodeExecutionStatus` dispatched via `GraphynEditorIntent.UpdateNodeExecutionStatus`.

```kotlin
sealed interface NodeExecutionStatus {
    data object Idle    : NodeExecutionStatus
    data object Running : NodeExecutionStatus
    data class  Success(val durationMs: Long) : NodeExecutionStatus
    data class  Error(val message: String)    : NodeExecutionStatus
}
```

Custom cards can include the badge via:

```kotlin
NodeStatusBadge(
    status = context.executionStatus,
    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
    surfaceColor = myCardBackground,
)
```
