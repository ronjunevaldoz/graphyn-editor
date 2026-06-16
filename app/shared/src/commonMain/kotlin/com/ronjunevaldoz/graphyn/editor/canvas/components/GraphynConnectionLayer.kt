package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import kotlin.math.absoluteValue

@Composable
fun GraphynConnectionLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
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
            val fromBounds = state.nodeBounds(fromNode.id, fromIndex)
            val toBounds = state.nodeBounds(toNode.id, toIndex)

            drawBezier(
                start = Offset(fromBounds.right, fromBounds.center.y),
                end = Offset(toBounds.left, toBounds.center.y),
                color = color,
                strokeWidth = 4f,
            )
        }

        val draftConnection = draft ?: return@Canvas
        val fromNode = workflow.nodes.firstOrNull { it.id == draftConnection.fromNodeId } ?: return@Canvas
        val fromIndex = workflow.nodes.indexOf(fromNode)
        val fromBounds = state.nodeBounds(fromNode.id, fromIndex)
        val start = Offset(
            x = fromBounds.right,
            y = fromBounds.center.y,
        )
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
