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
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.absoluteValue

@Composable
fun GraphynConnectionLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    draft: GraphynConnectionDraft?,
    draftPointer: Offset?,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: Color,
) {
    Canvas(modifier = modifier) {
        val nodeWidthPx = GraphynCanvasMetrics.NodeSize.width.dp.toPx()

        workflow.connections.forEach { connection ->
            val fromNode = workflow.nodes.firstOrNull { it.id == connection.fromNodeId } ?: return@forEach
            val toNode = workflow.nodes.firstOrNull { it.id == connection.toNodeId } ?: return@forEach
            val fromIndex = workflow.nodes.indexOf(fromNode)
            val toIndex = workflow.nodes.indexOf(toNode)
            val fromPos = state.nodePosition(fromNode.id, fromIndex)
            val toPos = state.nodePosition(toNode.id, toIndex)

            val fromSpec = nodeSpecs.resolve(fromNode.type)
            val fromPortIndex = fromSpec?.outputs?.indexOfFirst { it.name == connection.fromPort }?.coerceAtLeast(0) ?: 0
            val fromY = fromPos.y + GraphynCanvasMetrics.portAnchorY(fromPortIndex).dp.toPx()

            val toSpec = nodeSpecs.resolve(toNode.type)
            val toPortIndex = toSpec?.inputs?.indexOfFirst { it.name == connection.toPort }?.coerceAtLeast(0) ?: 0
            val toY = toPos.y + GraphynCanvasMetrics.portAnchorY(toPortIndex).dp.toPx()

            drawBezier(
                start = Offset(fromPos.x + nodeWidthPx, fromY),
                end = Offset(toPos.x.toFloat(), toY),
                color = color,
                strokeWidth = 4f,
            )
        }

        val draftConnection = draft ?: return@Canvas
        val fromNode = workflow.nodes.firstOrNull { it.id == draftConnection.fromNodeId } ?: return@Canvas
        val fromIndex = workflow.nodes.indexOf(fromNode)
        val fromPos = state.nodePosition(fromNode.id, fromIndex)
        val fromSpec = nodeSpecs.resolve(fromNode.type)
        val fromPortIndex = if (draftConnection.isFromInput) {
            fromSpec?.inputs?.indexOfFirst { it.name == draftConnection.fromPort }?.coerceAtLeast(0) ?: 0
        } else {
            fromSpec?.outputs?.indexOfFirst { it.name == draftConnection.fromPort }?.coerceAtLeast(0) ?: 0
        }
        val fromY = fromPos.y + GraphynCanvasMetrics.portAnchorY(fromPortIndex).dp.toPx()
        val start = if (draftConnection.isFromInput) {
            Offset(fromPos.x.toFloat(), fromY)
        } else {
            Offset(fromPos.x + nodeWidthPx, fromY)
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
