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
import com.ronjunevaldoz.graphyn.editor.canvas.resolvePortAnchor
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

        val fromAnchor = resolvePortAnchor(fromNode, connection.fromPort, isInput = false, nodeSpecs, canvasCards, workflow)
        val toAnchor = resolvePortAnchor(toNode, connection.toPort, isInput = true, nodeSpecs, canvasCards, workflow)

        val isSelected = state.selectedConnection == connection
        val dotColor = if (isSelected) selectedColor else connectionColor
        Box(
            modifier = Modifier
                .testTag("connection-midpoint-${connection.fromNodeId}-${connection.fromPort}")
                .offset {
                    val dotRadiusPx = GraphynCanvasMetrics.PortDotRadius.dp.roundToPx()
                    val fromX = fromPos.x + fromAnchor.nodeWidthDp.dp.roundToPx()
                    val fromY = fromPos.y + fromAnchor.anchorYDp.dp.roundToPx()
                    val toY = toPos.y + toAnchor.anchorYDp.dp.roundToPx()
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
