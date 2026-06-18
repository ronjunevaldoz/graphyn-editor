package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasLayout
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

internal class GraphynNodeLayoutState(
    private val canvasBounds: GraphynCanvasBounds,
    private val viewportScale: () -> Float,
) {
    var nodePositionsByNodeId by mutableStateOf<Map<String, IntOffset>>(emptyMap())
    private val nodeDragRemaindersByNodeId = mutableMapOf<String, Offset>()

    fun setNodePosition(nodeId: String, position: IntOffset, clearDragRemainder: Boolean = true) {
        nodePositionsByNodeId = nodePositionsByNodeId + (nodeId to clamp(nodeId, position))
        if (clearDragRemainder) nodeDragRemaindersByNodeId.remove(nodeId)
    }

    fun moveNode(nodeId: String, delta: IntOffset) {
        val current = nodePositionsByNodeId[nodeId] ?: IntOffset.Zero
        val remainder = nodeDragRemaindersByNodeId[nodeId] ?: Offset.Zero
        // delta is already in world space — the graphicsLayer viewport transform maps
        // screen pixels to local coordinates before the drag handler fires, so dividing
        // by scale here would apply the correction twice.
        val worldDelta = Offset(delta.x.toFloat() + remainder.x, delta.y.toFloat() + remainder.y)
        val applied = IntOffset(worldDelta.x.roundToInt(), worldDelta.y.roundToInt())
        nodeDragRemaindersByNodeId[nodeId] = Offset(worldDelta.x - applied.x, worldDelta.y - applied.y)
        setNodePosition(
            nodeId = nodeId,
            position = IntOffset(current.x + applied.x, current.y + applied.y),
            clearDragRemainder = false,
        )
    }

    fun removeNode(nodeId: String) {
        nodePositionsByNodeId = nodePositionsByNodeId - nodeId
        nodeDragRemaindersByNodeId.remove(nodeId)
    }

    fun nodePosition(nodeId: String, index: Int): IntOffset =
        nodePositionsByNodeId[nodeId] ?: GraphynCanvasLayout.fallbackPosition(index)

    fun nodeSize(): IntSize = GraphynCanvasMetrics.NodeSize

    fun nodeBounds(nodeId: String, index: Int): Rect {
        val pos = nodePosition(nodeId, index)
        val size = nodeSize()
        return Rect(pos.x.toFloat(), pos.y.toFloat(), pos.x.toFloat() + size.width, pos.y.toFloat() + size.height)
    }

    fun isOverNode(worldPosition: Offset, nodeIds: List<Pair<String, Int>>): Boolean =
        nodeIds.any { (id, idx) -> nodeBounds(id, idx).contains(worldPosition) }

    private fun clamp(nodeId: String, position: IntOffset): IntOffset {
        val size = nodeSize()
        return IntOffset(
            x = position.x.coerceIn(0, (canvasBounds.width - size.width).coerceAtLeast(0)),
            y = position.y.coerceIn(0, (canvasBounds.height - size.height).coerceAtLeast(0)),
        )
    }
}

internal fun buildNodeId(spec: NodeSpec, nodes: List<NodeRef>): String {
    val prefix = spec.type.substringAfterLast('.').ifBlank { "node" }
    val existing = nodes.mapTo(mutableSetOf()) { it.id }
    var suffix = 1
    while ("$prefix-$suffix" in existing) suffix++
    return "$prefix-$suffix"
}

