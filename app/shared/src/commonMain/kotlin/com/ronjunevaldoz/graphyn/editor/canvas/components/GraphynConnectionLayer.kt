package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.resolvePortAnchor
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.absoluteValue

@Composable
fun GraphynConnectionLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    canvasCards: NodeCanvasRegistry?,
    draft: GraphynConnectionDraft?,
    draftPointer: Offset?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        workflow.connections.forEach { connection ->
            val fromNode = workflow.nodes.firstOrNull { it.id == connection.fromNodeId } ?: return@forEach
            val toNode = workflow.nodes.firstOrNull { it.id == connection.toNodeId } ?: return@forEach
            val fromIndex = workflow.nodes.indexOf(fromNode)
            val toIndex = workflow.nodes.indexOf(toNode)
            val fromPos = state.nodePosition(fromNode.id, fromIndex)
            val toPos = state.nodePosition(toNode.id, toIndex)

            val fromAnchor = resolvePortAnchor(fromNode, connection.fromPort, isInput = false, nodeSpecs, canvasCards)
            val toAnchor = resolvePortAnchor(toNode, connection.toPort, isInput = true, nodeSpecs, canvasCards)

            drawBezier(
                start = Offset(fromPos.x + fromAnchor.nodeWidthDp.dp.toPx(), fromPos.y + fromAnchor.anchorYDp.dp.toPx()),
                end = Offset(toPos.x.toFloat(), toPos.y + toAnchor.anchorYDp.dp.toPx()),
                color = color,
                strokeWidth = 4f,
            )
        }

        val draftConnection = draft ?: return@Canvas
        val fromNode = workflow.nodes.firstOrNull { it.id == draftConnection.fromNodeId } ?: return@Canvas
        val fromIndex = workflow.nodes.indexOf(fromNode)
        val fromPos = state.nodePosition(fromNode.id, fromIndex)
        val fromAnchor = resolvePortAnchor(
            fromNode, draftConnection.fromPort, isInput = draftConnection.isFromInput, nodeSpecs, canvasCards,
        )

        val start = if (draftConnection.isFromInput) {
            Offset(fromPos.x.toFloat(), fromPos.y + fromAnchor.anchorYDp.dp.toPx())
        } else {
            Offset(fromPos.x + fromAnchor.nodeWidthDp.dp.toPx(), fromPos.y + fromAnchor.anchorYDp.dp.toPx())
        }
        val end = draftPointer ?: Offset(start.x + 120f, start.y)
        drawBezier(
            start = start,
            end = end,
            color = color.copy(alpha = 0.35f),
            strokeWidth = 3f,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBezier(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
) {
    val distance = (end.x - start.x).absoluteValue.coerceAtLeast(120f)
    val direction = if (end.x >= start.x) 1f else -1f
    val control = distance * 0.35f
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + (control * direction),
            start.y,
            end.x - (control * direction),
            end.y,
            end.x,
            end.y,
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth),
    )
}
