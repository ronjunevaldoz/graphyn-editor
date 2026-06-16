package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.calculateMinimapLayout
import com.ronjunevaldoz.graphyn.editor.state.calculateViewportRectInMinimap
import com.ronjunevaldoz.graphyn.editor.state.constrainTo
import com.ronjunevaldoz.graphyn.editor.state.mapMinimapPointToWorld
import com.ronjunevaldoz.graphyn.editor.state.viewportCenteredOnWorldPoint

@Composable
internal fun GraphynMinimapDebugger(
    state: GraphynEditorState,
    modifier: Modifier = Modifier,
) {
    val workflow = state.workflow
    val nodeCount = workflow?.nodes?.size ?: 0
    val minimapColors = rememberMinimapColors()
    var minimapSize by remember { mutableStateOf(IntSize.Zero) }
    val nodePositions = remember(workflow, state.nodePositionsByNodeId) {
        workflow?.nodes.orEmpty().mapIndexed { index, node ->
            state.nodePosition(node.id, index)
        }
    }
    val graphWorldBounds = state.graphWorldBounds
    val minimapLayout = remember(graphWorldBounds, minimapSize) {
        val worldBounds = graphWorldBounds ?: return@remember null
        calculateMinimapLayout(
            worldBounds = worldBounds,
            minimapSize = minimapSize,
        )
    }
    Canvas(
        modifier = modifier
            .onSizeChanged { minimapSize = it }
            .pointerInput(minimapLayout, state.canvasSize) {
                val layout = minimapLayout ?: return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        val worldPoint = mapMinimapPointToWorld(
                            minimapPoint = start,
                            layout = layout,
                        )
                        state.viewport = viewportCenteredOnWorldPoint(
                            currentViewport = state.viewport,
                            canvasSize = state.canvasSize,
                            worldPoint = worldPoint,
                        ).constrainTo(state.graphWorldBounds, state.canvasSize)
                    },
                ) { change, _ ->
                    change.consume()
                    val worldPoint = mapMinimapPointToWorld(
                        minimapPoint = change.position,
                        layout = layout,
                    )
                    state.viewport = viewportCenteredOnWorldPoint(
                        currentViewport = state.viewport,
                        canvasSize = state.canvasSize,
                        worldPoint = worldPoint,
                    ).constrainTo(state.graphWorldBounds, state.canvasSize)
                }
            },
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(color = minimapColors.background)

        if (nodePositions.isEmpty()) {
            drawRect(
                color = minimapColors.emptyStroke,
                style = Stroke(width = 1f),
            )
            return@Canvas
        }

        val layout = minimapLayout

        if (layout == null) {
            drawRect(
                color = minimapColors.emptyStroke,
                style = Stroke(width = 1f),
            )
            return@Canvas
        }

        nodePositions.forEach { position ->
            val x = layout.insetX + ((position.x.toFloat() - layout.worldBounds.left) * layout.scale)
            val y = layout.insetY + ((position.y.toFloat() - layout.worldBounds.top) * layout.scale)
            val width = GraphynCanvasMetrics.NodeSize.width * layout.scale * 2f
            val height = GraphynCanvasMetrics.NodeSize.height * layout.scale * 2f
            drawRect(
                color = minimapColors.nodeFill,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(width, height),
            )
            drawRect(
                color = minimapColors.nodeStroke,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(width, height),
                style = Stroke(width = 1.5f),
            )
        }

        calculateViewportRectInMinimap(
            viewport = state.viewport,
            canvasSize = state.canvasSize,
            layout = layout,
        )?.let { viewportRect ->
            val cameraColor = minimapColors.viewportStroke
            val topLeft = Offset(viewportRect.left, viewportRect.top)
            val size = androidx.compose.ui.geometry.Size(
                viewportRect.width,
                viewportRect.height,
            )
            drawRect(
                color = cameraColor,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 2f),
            )
            val handleRadius = 2.25f
            listOf(
                topLeft,
                Offset(topLeft.x + size.width, topLeft.y),
                Offset(topLeft.x, topLeft.y + size.height),
                Offset(topLeft.x + size.width, topLeft.y + size.height),
            ).forEach { corner ->
                drawCircle(
                    color = cameraColor,
                    radius = handleRadius,
                    center = corner,
                )
            }
        }
    }
}

@Composable
private fun rememberMinimapColors(): MiniMapColors = MiniMapColors(
    background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
    emptyStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
    nodeFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
    nodeStroke = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
    viewportStroke = MaterialTheme.colorScheme.secondary.copy(alpha = 0.96f),
)

private data class MiniMapColors(
    val background: Color,
    val emptyStroke: Color,
    val nodeFill: Color,
    val nodeStroke: Color,
    val viewportStroke: Color,
)
