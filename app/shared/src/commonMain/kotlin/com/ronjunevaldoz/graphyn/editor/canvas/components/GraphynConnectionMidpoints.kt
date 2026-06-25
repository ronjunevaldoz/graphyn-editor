package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.resolveNodeFactory
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynConnectionMidpoints(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    canvasCards: NodeCanvasRegistry?,
    connectionColor: Color,
    selectedColor: Color,
    surfaceColor: Color,
) {
    if (state.connectionDraft != null) return
    workflow.connections.forEach { connection ->
        val fromNode = workflow.nodes.firstOrNull { it.id == connection.fromNodeId } ?: return@forEach
        val toNode = workflow.nodes.firstOrNull { it.id == connection.toNodeId } ?: return@forEach
        val fromIndex = workflow.nodes.indexOf(fromNode)
        val toIndex = workflow.nodes.indexOf(toNode)
        val fromPos = state.nodePosition(fromNode.id, fromIndex)
        val toPos = state.nodePosition(toNode.id, toIndex)

        val fromSpec = nodeSpecs.resolve(fromNode.type)
        val fromFactory = resolveNodeFactory(fromNode, canvasCards, nodeSpecs)
        val fromPortIndex = fromSpec?.outputs?.indexOfFirst { it.name == connection.fromPort }?.coerceAtLeast(0) ?: 0
        val fromNodeWidth = fromFactory?.nodeWidth ?: GraphynCanvasMetrics.NodeSize.width
        val fromAnchorY = fromSpec?.let {
            fromFactory?.portAnchorY(fromPortIndex, false, it) ?: GraphynCanvasMetrics.portAnchorY(fromPortIndex)
        } ?: GraphynCanvasMetrics.portAnchorY(fromPortIndex)

        val toSpec = nodeSpecs.resolve(toNode.type)
        val toFactory = resolveNodeFactory(toNode, canvasCards, nodeSpecs)
        val toPortIndex = toSpec?.inputs?.indexOfFirst { it.name == connection.toPort }?.coerceAtLeast(0) ?: 0
        val toAnchorY = toSpec?.let {
            toFactory?.portAnchorY(toPortIndex, true, it) ?: GraphynCanvasMetrics.portAnchorY(toPortIndex)
        } ?: GraphynCanvasMetrics.portAnchorY(toPortIndex)

        val isSelected = state.selectedConnection == connection
        val dotColor = if (isSelected) selectedColor else connectionColor
        Box(
            modifier = Modifier
                .testTag("connection-midpoint-${connection.fromNodeId}-${connection.fromPort}")
                .offset {
                    val dotRadiusPx = GraphynCanvasMetrics.PortDotRadius.dp.roundToPx()
                    val fromX = fromPos.x + fromNodeWidth.dp.roundToPx()
                    val fromY = fromPos.y + fromAnchorY.dp.roundToPx()
                    val toY = toPos.y + toAnchorY.dp.roundToPx()
                    IntOffset(
                        x = (fromX + toPos.x) / 2 - dotRadiusPx,
                        y = (fromY + toY) / 2 - dotRadiusPx,
                    )
                }
                .size(GraphynCanvasMetrics.PortDotDiameter.dp)
                .clip(CircleShape)
                .background(if (isSelected) dotColor.copy(alpha = 0.15f) else surfaceColor)
                .border(2.dp, dotColor, CircleShape)
                .clickable {
                    state.dispatch(GraphynEditorIntent.SelectConnection(if (isSelected) null else connection))
                },
        )
    }
}
