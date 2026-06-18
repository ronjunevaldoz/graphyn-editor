package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphynViewportMathTest {
    @Test
    fun panByMovesTheViewportOffset() {
        val viewport = GraphynViewport(offset = Offset(120f, 80f))

        val moved = viewport.panBy(Offset(-20f, 15f))

        assertEquals(Offset(100f, 95f), moved.offset)
        assertEquals(viewport.scale, moved.scale)
    }

    @Test
    fun zoomAtKeepsTheFocusPointStable() {
        val viewport = GraphynViewport(
            offset = Offset(100f, 40f),
            scale = 1f,
        )
        val focus = Offset(240f, 180f)
        val worldBefore = viewport.screenToWorld(focus)

        val zoomed = viewport.zoomAt(
            focus = focus,
            factor = 1.5f,
            minScale = 0.45f,
            maxScale = 2.75f,
        )

        assertEquals(1.5f, zoomed.scale)
        assertEquals(worldBefore.x, zoomed.screenToWorld(focus).x, 0.0001f)
        assertEquals(worldBefore.y, zoomed.screenToWorld(focus).y, 0.0001f)
    }

    @Test
    fun minimapLayoutAndViewportRectStayBounded() {
        val layout = calculateMinimapLayout(
            nodePositions = listOf(
                IntOffset(0, 0),
                IntOffset(304, 0),
                IntOffset(624, 220),
            ),
            nodeSize = IntSize(280, 180),
            minimapSize = IntSize(240, 160),
        )

        assertTrue(layout.scale > 0f)
        assertTrue(layout.insetX >= 0f)
        assertTrue(layout.insetY >= 0f)

        val viewportRect = calculateViewportRectInMinimap(
            viewport = GraphynViewport(
                offset = Offset(120f, 80f),
                scale = 1.15f,
            ),
            canvasSize = IntSize(960, 720),
            layout = layout,
        )

        assertNotNull(viewportRect)
        assertTrue(viewportRect.width > 0f)
        assertTrue(viewportRect.height > 0f)
        assertTrue(viewportRect.left.isFinite())
        assertTrue(viewportRect.top.isFinite())
    }

    @Test
    fun minimapPointCanRecenterTheViewport() {
        val layout = calculateMinimapLayout(
            nodePositions = listOf(
                IntOffset(0, 0),
                IntOffset(304, 0),
            ),
            nodeSize = IntSize(280, 180),
            minimapSize = IntSize(240, 160),
        )
        val minimapPoint = Offset(
            x = layout.insetX + 72f,
            y = layout.insetY + 48f,
        )
        val worldPoint = mapMinimapPointToWorld(minimapPoint, layout)
        val viewport = GraphynViewport(offset = Offset(20f, 30f), scale = 1.25f)
        val centered = viewportCenteredOnWorldPoint(
            currentViewport = viewport,
            canvasSize = IntSize(960, 720),
            worldPoint = worldPoint,
        )

        assertEquals(worldPoint.x, centered.screenToWorld(Offset(480f, 360f)).x, 0.0001f)
        assertEquals(worldPoint.y, centered.screenToWorld(Offset(480f, 360f)).y, 0.0001f)
    }

    @Test
    fun hitTestDetectsNodeBoundsInWorldSpace() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow",
                name = "HitTest",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
        )
        state.setNodePosition("logger-1", IntOffset(100, 120))

        assertTrue(state.isWorldPositionOverNode(Offset(150f, 160f)))
        assertTrue(!state.isWorldPositionOverNode(Offset(10f, 10f)))
    }

    @Test
    fun moveNodeAppliesDeltaDirectlyAsWorldSpaceOffset() {
        // graphicsLayer already normalises screen deltas by viewport scale before
        // the drag handler fires, so MoveNode receives world-space deltas and
        // moveNode must NOT divide again.
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-move",
                name = "Move",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
        )
        state.setNodePosition("logger-1", IntOffset(100, 120))
        state.viewport = GraphynViewport(scale = 0.5f, offset = Offset(0f, 0f))

        state.dispatch(GraphynEditorIntent.MoveNode("logger-1", IntOffset(10, 20)))

        assertEquals(IntOffset(110, 140), state.nodePosition("logger-1", 0))
    }

    @Test
    fun repeatedSmallNodeDragsAccumulateWithoutDrift() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-move-linear",
                name = "MoveLinear",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
        )
        state.setNodePosition("logger-1", IntOffset(100, 120))
        state.viewport = GraphynViewport(scale = 0.75f, offset = Offset(0f, 0f))

        repeat(3) {
            state.dispatch(GraphynEditorIntent.MoveNode("logger-1", IntOffset(1, 1)))
        }

        assertEquals(IntOffset(103, 123), state.nodePosition("logger-1", 0))
    }

    @Test
    fun minimapWorldBoundsStayStableWhenNodeMovesInsideTheExistingFrame() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-bounds",
                name = "Bounds",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                    NodeRef(id = "logger-2", type = "logger"),
                ),
                connections = emptyList(),
            ),
        )
        state.setNodePosition("logger-1", IntOffset(100, 120))
        state.setNodePosition("logger-2", IntOffset(460, 180))

        val initialBounds: Rect = state.graphWorldBounds ?: error("Expected world bounds")
        val viewport = GraphynViewport(offset = Offset(140f, 90f), scale = 1.1f)
        val canvasSize = IntSize(960, 720)
        val initialLayout = calculateMinimapLayout(
            worldBounds = initialBounds,
            minimapSize = IntSize(240, 160),
        )
        val initialViewportRect = calculateViewportRectInMinimap(
            viewport = viewport,
            canvasSize = canvasSize,
            layout = initialLayout,
        )

        state.dispatch(GraphynEditorIntent.MoveNode("logger-1", IntOffset(8, 6)))

        assertEquals(initialBounds, state.graphWorldBounds)

        val nextLayout = calculateMinimapLayout(
            worldBounds = state.graphWorldBounds ?: error("Expected world bounds"),
            minimapSize = IntSize(240, 160),
        )
        val nextViewportRect = calculateViewportRectInMinimap(
            viewport = viewport,
            canvasSize = canvasSize,
            layout = nextLayout,
        )

        assertEquals(initialViewportRect, nextViewportRect)
    }

    @Test
    fun graphWorldBoundsUsesTheConfiguredCanvasFrame() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-frame",
                name = "Frame",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
            canvasBounds = GraphynCanvasBounds(width = 1600, height = 900),
        )

        assertEquals(Rect(0f, 0f, 1600f, 900f), state.graphWorldBounds)
    }

    @Test
    fun viewportTransformIsClampedToTheConfiguredCanvasFrame() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-viewport-clamp",
                name = "ViewportClamp",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
            canvasBounds = GraphynCanvasBounds(width = 1600, height = 900),
        )
        state.updateCanvasSize(IntSize(960, 720))
        state.viewport = GraphynViewport(offset = Offset(1200f, 800f), scale = 1f)

        state.dispatch(GraphynEditorIntent.UpdateViewportTransform(
            pan = Offset.Zero,
            zoom = 1f,
            focus = Offset.Zero,
        ))

        assertTrue(state.viewport.offset.x <= 0f)
        assertTrue(state.viewport.offset.y <= 0f)
        assertTrue(state.viewport.screenToWorld(Offset.Zero).x >= 0f)
        assertTrue(state.viewport.screenToWorld(Offset.Zero).y >= 0f)
    }

    @Test
    fun nodePositionsAreClampedInsideTheConfiguredCanvasFrame() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-node-clamp",
                name = "NodeClamp",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                ),
                connections = emptyList(),
            ),
            canvasBounds = GraphynCanvasBounds(width = 640, height = 360),
        )

        state.setNodePosition("logger-1", IntOffset(520, 260))

        assertEquals(IntOffset(360, 180), state.nodePosition("logger-1", 0))
    }
}
