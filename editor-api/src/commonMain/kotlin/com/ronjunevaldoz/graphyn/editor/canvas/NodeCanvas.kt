package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec

@Stable
data class NodeCanvasContext(
    val node: NodeRef,
    val spec: NodeSpec,
    val selected: Boolean,
    val executionStatus: NodeExecutionStatus,
    val onSelect: () -> Unit,
    val onMove: (IntOffset) -> Unit,
    /** Canvas surface text color — use for labels drawn on the canvas background, not on the card itself. */
    val contentColor: Color = Color.Unspecified,
)

private const val DEFAULT_PORT_SECTION_TOP = 70
private const val DEFAULT_PORT_LABEL_H = 16
private const val DEFAULT_PORT_LABEL_GAP = 6
private const val DEFAULT_PORT_BUBBLE_H = 32
private const val DEFAULT_PORT_BUBBLE_GAP = 8
private const val DEFAULT_NODE_WIDTH = 280

interface NodeCanvasFactory {
    @Composable
    fun NodeCanvas(context: NodeCanvasContext)

    /** Node card width in dp — used to position the right-edge output dots. */
    val nodeWidth: Int get() = DEFAULT_NODE_WIDTH

    /**
     * Y offset in dp from the node's top-left corner to the centre of the
     * port dot for [portIndex] (input or output).  Override to match your
     * card's actual layout.
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
