package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val minimapColors = rememberMinimapColors()
    var minimapSize by remember { mutableStateOf(IntSize.Zero) }
    val nodePositions = remember(workflow, state.nodePositionsByNodeId) {
        workflow?.nodes.orEmpty().mapIndexed { index, node -> state.nodePosition(node.id, index) }
    }
    val graphWorldBounds = state.graphWorldBounds
    val minimapLayout = remember(graphWorldBounds, minimapSize) {
        val worldBounds = graphWorldBounds ?: return@remember null
        calculateMinimapLayout(worldBounds = worldBounds, minimapSize = minimapSize)
    }
    Canvas(
        modifier = modifier
            .onSizeChanged { minimapSize = it }
            .pointerInput(minimapLayout, state.canvasSize) {
                val layout = minimapLayout ?: return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        state.viewport = viewportCenteredOnWorldPoint(
                            currentViewport = state.viewport,
                            canvasSize = state.canvasSize,
                            worldPoint = mapMinimapPointToWorld(start, layout),
                        ).constrainTo(state.graphWorldBounds, state.canvasSize)
                    },
                ) { change, _ ->
                    change.consume()
                    state.viewport = viewportCenteredOnWorldPoint(
                        currentViewport = state.viewport,
                        canvasSize = state.canvasSize,
                        worldPoint = mapMinimapPointToWorld(change.position, layout),
                    ).constrainTo(state.graphWorldBounds, state.canvasSize)
                }
            },
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(color = minimapColors.background)

        if (nodePositions.isEmpty() || minimapLayout == null) {
            drawRect(color = minimapColors.emptyStroke, style = Stroke(width = 1f))
            return@Canvas
        }

        nodePositions.forEach { position ->
            val x = minimapLayout.insetX + ((position.x.toFloat() - minimapLayout.worldBounds.left) * minimapLayout.scale)
            val y = minimapLayout.insetY + ((position.y.toFloat() - minimapLayout.worldBounds.top) * minimapLayout.scale)
            val width = GraphynCanvasMetrics.NodeSize.width * minimapLayout.scale * 2f
            val height = GraphynCanvasMetrics.NodeSize.height * minimapLayout.scale * 2f
            val nodeSize = androidx.compose.ui.geometry.Size(width, height)
            drawRect(color = minimapColors.nodeFill, topLeft = Offset(x, y), size = nodeSize)
            drawRect(color = minimapColors.nodeStroke, topLeft = Offset(x, y), size = nodeSize, style = Stroke(width = 1.5f))
        }

        calculateViewportRectInMinimap(
            viewport = state.viewport,
            canvasSize = state.canvasSize,
            layout = minimapLayout,
        )?.let { viewportRect ->
            val topLeft = Offset(viewportRect.left, viewportRect.top)
            val rectSize = androidx.compose.ui.geometry.Size(viewportRect.width, viewportRect.height)
            drawRect(color = minimapColors.viewportStroke, topLeft = topLeft, size = rectSize, style = Stroke(width = 2f))
            listOf(
                topLeft,
                Offset(topLeft.x + rectSize.width, topLeft.y),
                Offset(topLeft.x, topLeft.y + rectSize.height),
                Offset(topLeft.x + rectSize.width, topLeft.y + rectSize.height),
            ).forEach { corner ->
                drawCircle(color = minimapColors.viewportStroke, radius = 2.25f, center = corner)
            }
        }
    }
}
