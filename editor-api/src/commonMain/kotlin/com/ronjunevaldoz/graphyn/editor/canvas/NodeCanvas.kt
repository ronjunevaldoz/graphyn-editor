package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * All runtime data a card composable needs at render time.
 *
 * [onConfigChange] is called when the user edits a field value on the card.
 * The editor dispatches [GraphynEditorIntent.UpdateNodeConfig] in response.
 */
@Stable
data class NodeCanvasContext(
    val node: NodeRef,
    val spec: NodeSpec,
    val selected: Boolean,
    val executionStatus: NodeExecutionStatus,
    val onSelect: () -> Unit,
    val onMove: (IntOffset) -> Unit,
    val onConfigChange: (key: String, value: WorkflowValue) -> Unit = { _, _ -> },
    /** Canvas surface text color — use for labels drawn on the canvas background, not on the card itself. */
    val contentColor: Color = Color.Unspecified,
    /** Non-null only when this node has an embedded [WorkflowDefinition] and the host supports drill-in navigation. */
    val onEnterSubgraph: (() -> Unit)? = null,
    /** Output values produced by the last workflow run for this node. Empty before any run. */
    val executionOutputs: Map<String, WorkflowValue> = emptyMap(),
)

enum class NodeShape { Rectangle, Circle }

private const val DEFAULT_PORT_SECTION_TOP = 70
private const val DEFAULT_PORT_LABEL_H = 16
private const val DEFAULT_PORT_LABEL_GAP = 6
private const val DEFAULT_PORT_BUBBLE_H = 32
private const val DEFAULT_PORT_BUBBLE_GAP = 8
private const val DEFAULT_NODE_WIDTH = 280
private const val DEFAULT_NODE_HEIGHT = 180

/**
 * Contract for a node card renderer. Implement this and register it via
 * [GraphynEditorPluginRegistrar.registerCanvasCard] to give a node type a custom appearance.
 *
 * The canvas uses [nodeWidth], [nodeHeight], and [portAnchorY] to draw connection wires
 * and render the minimap — override all three to match your card's actual layout.
 */
interface NodeCanvasFactory {
    @Composable
    fun NodeCanvas(context: NodeCanvasContext)

    /** Card width in dp. */
    val nodeWidth: Int get() = DEFAULT_NODE_WIDTH

    /** Card height in dp. */
    val nodeHeight: Int get() = DEFAULT_NODE_HEIGHT

    /** Card shape used by the minimap renderer. */
    val nodeShape: NodeShape get() = NodeShape.Rectangle

    /**
     * When true the canvas renders this node beneath all regular nodes and the minimap skips it.
     * Use for annotation layers (sticky notes, frames) that should never occlude workflow nodes.
     */
    val isAnnotation: Boolean get() = false

    /**
     * Y offset in dp from the card's top-left corner to the centre of the connection dot
     * for [portIndex]. Override to align wires with your card's actual port row positions.
     */
    fun portAnchorY(portIndex: Int, isInput: Boolean, spec: NodeSpec): Int =
        DEFAULT_PORT_SECTION_TOP + DEFAULT_PORT_LABEL_H + DEFAULT_PORT_LABEL_GAP +
            DEFAULT_PORT_BUBBLE_H / 2 + portIndex * (DEFAULT_PORT_BUBBLE_H + DEFAULT_PORT_BUBBLE_GAP)
}

interface NodeCanvasRegistry {
    fun resolve(nodeType: String): NodeCanvasFactory?
    fun register(nodeType: String, factory: NodeCanvasFactory)
}

@GraphynExperimentalApi
class DefaultNodeCanvasRegistry : NodeCanvasRegistry {
    private val factories = mutableMapOf<String, NodeCanvasFactory>()
    override fun resolve(nodeType: String): NodeCanvasFactory? = factories[nodeType]
    override fun register(nodeType: String, factory: NodeCanvasFactory) { factories[nodeType] = factory }
}
