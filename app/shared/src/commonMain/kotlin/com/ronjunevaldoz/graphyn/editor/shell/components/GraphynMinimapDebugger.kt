package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeShape
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.calculateMinimapLayout
import com.ronjunevaldoz.graphyn.editor.state.calculateViewportRectInMinimap
import com.ronjunevaldoz.graphyn.editor.state.constrainTo
import com.ronjunevaldoz.graphyn.editor.state.mapMinimapPointToWorld
import com.ronjunevaldoz.graphyn.editor.state.viewportCenteredOnWorldPoint

private const val MINIMAP_PADDING = 40f

@Composable
internal fun GraphynMinimapDebugger(
    state: GraphynEditorState,
    canvasCards: NodeCanvasRegistry?,
    modifier: Modifier = Modifier,
) {
    val colors = GraphynDs.colors
    val minimapColors = rememberMinimapColors()
    val shape = RoundedCornerShape(6.dp)
    var minimapSize by remember { mutableStateOf(IntSize.Zero) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(state.viewport) {
        alpha.animateTo(0.9f, tween(durationMillis = 150))
        delay(1500L)
        alpha.animateTo(0f, tween(durationMillis = 600))
    }
    val workflow = state.workflow
    val nodePositions = remember(workflow, state.nodePositionsByNodeId) {
        workflow?.nodes.orEmpty().mapIndexed { index, node -> state.nodePosition(node.id, index) }
    }
    val graphWorldBounds = state.graphWorldBounds
    val minimapLayout = remember(graphWorldBounds, minimapSize) {
        val worldBounds = graphWorldBounds ?: return@remember null
        calculateMinimapLayout(worldBounds = worldBounds.inflate(MINIMAP_PADDING), minimapSize = minimapSize)
    }

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha.value }
            .clip(shape)
            .background(minimapColors.background)
            .border(1.dp, colors.border, shape)
            .padding(4.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("minimap-canvas")
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

            if (nodePositions.isEmpty() || minimapLayout == null) {
                drawRect(color = minimapColors.emptyStroke, style = Stroke(width = 1f))
                return@Canvas
            }

            val nodes = workflow?.nodes.orEmpty()
            nodes.forEachIndexed { index, node ->
                val position = nodePositions.getOrNull(index) ?: return@forEachIndexed
                val factory = canvasCards?.resolve(node.type)
                if (factory?.isAnnotation == true) return@forEachIndexed
                val nodeW = (factory?.nodeWidth ?: GraphynCanvasMetrics.NodeSize.width).toFloat()
                val nodeH = (factory?.nodeHeight ?: GraphynCanvasMetrics.NodeSize.height).toFloat()
                val shape = factory?.nodeShape ?: NodeShape.Rectangle
                val x = minimapLayout.insetX + ((position.x.toFloat() - minimapLayout.worldBounds.left) * minimapLayout.scale)
                val y = minimapLayout.insetY + ((position.y.toFloat() - minimapLayout.worldBounds.top) * minimapLayout.scale)
                val w = nodeW * minimapLayout.scale * 2f
                val h = nodeH * minimapLayout.scale * 2f
                when (shape) {
                    NodeShape.Circle -> {
                        val radius = w / 2f
                        val center = Offset(x + radius, y + radius)
                        drawCircle(color = minimapColors.nodeFill, radius = radius, center = center)
                        drawCircle(color = minimapColors.nodeStroke, radius = radius, center = center, style = Stroke(width = 1.5f))
                    }
                    NodeShape.Rectangle -> {
                        val nodeSize = Size(w, h)
                        drawRect(color = minimapColors.nodeFill, topLeft = Offset(x, y), size = nodeSize)
                        drawRect(color = minimapColors.nodeStroke, topLeft = Offset(x, y), size = nodeSize, style = Stroke(width = 1.5f))
                    }
                }
            }

            calculateViewportRectInMinimap(
                viewport = state.viewport,
                canvasSize = state.canvasSize,
                layout = minimapLayout,
            )?.let { vp ->
                val topLeft = Offset(vp.left, vp.top)
                val rectSize = Size(vp.width, vp.height)
                drawRect(color = minimapColors.viewportFill, topLeft = topLeft, size = rectSize)
                drawRect(color = minimapColors.viewportStroke, topLeft = topLeft, size = rectSize, style = Stroke(width = 2f))
            }
        }
    }
}
