package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitToContentTest {

    private fun makeState(vararg nodeIds: String) = GraphynEditorState(
        WorkflowDefinition(
            id = "fit-test",
            name = "Fit",
            nodes = nodeIds.map { NodeRef(id = it, type = "test") },
            connections = emptyList(),
        )
    )

    // Three-node layout: format(240×103) — gap 160 — script(320×248) — gap 160 — preview(180×144)
    // Total width = 1060, centre at canvas centre (2048, 1536)
    private val scriptPositions = mapOf(
        "format"  to IntOffset(1518, 1484),
        "script"  to IntOffset(1918, 1412),
        "preview" to IntOffset(2398, 1464),
    )
    private val scriptSizes = mapOf(
        "format"  to IntSize(240, 103),
        "script"  to IntSize(320, 248),
        "preview" to IntSize(180, 144),
    )

    @Test
    fun scaleIsAtMostOneAfterFitToContent() {
        val state = makeState("format", "script", "preview")
        state.updateCanvasSize(IntSize(1200, 800))

        state.fitToContent(scriptPositions, scriptSizes)

        assertTrue(state.viewport.scale <= 1.0f,
            "scale should be ≤ 1.0f, was ${state.viewport.scale}")
    }

    @Test
    fun contentCenterMapsToScreenCenterAfterFitToContent() {
        val state = makeState("format", "script", "preview")
        state.updateCanvasSize(IntSize(1200, 800))

        state.fitToContent(scriptPositions, scriptSizes)

        // bbox: minX=1518 maxX=2578  minY=1412 maxY=1660  → center=(2048, 1536)
        val screenPos = state.worldToScreen(Offset(2048f, 1536f))

        val tolerance = 0.5f
        assertTrue(abs(screenPos.x - 600f) < tolerance,
            "content centre X should map to screen X=600, was ${screenPos.x}")
        assertTrue(abs(screenPos.y - 400f) < tolerance,
            "content centre Y should map to screen Y=400, was ${screenPos.y}")
    }

    @Test
    fun retinaCanvasDoesNotBlowUpScale() {
        val state = makeState("format", "script", "preview")
        // Simulate canvasSize arriving in physical pixels on a 2× Retina display
        state.updateCanvasSize(IntSize(4000, 3000))

        state.fitToContent(scriptPositions, scriptSizes)

        assertTrue(state.viewport.scale <= 1.0f,
            "Retina pixel canvas must not produce scale > 1.0f, was ${state.viewport.scale}")
    }

    @Test
    fun singleNodeIsCenteredAfterFitToContent() {
        val state = makeState("script")
        state.updateCanvasSize(IntSize(1200, 800))
        val positions = mapOf("script" to IntOffset(1908, 1412))
        val sizes = mapOf("script" to IntSize(320, 248))

        state.fitToContent(positions, sizes)

        // bbox centre = (1908 + 160, 1412 + 124) = (2068, 1536)
        val screenPos = state.worldToScreen(Offset(2068f, 1536f))

        val tolerance = 0.5f
        assertTrue(abs(screenPos.x - 600f) < tolerance,
            "single-node centre X should map to screen X=600, was ${screenPos.x}")
        assertTrue(abs(screenPos.y - 400f) < tolerance,
            "single-node centre Y should map to screen Y=400, was ${screenPos.y}")
    }

    @Test
    fun emptyPositionsAreANoOp() {
        val state = makeState("n1")
        state.updateCanvasSize(IntSize(1200, 800))
        val before = state.viewport

        state.fitToContent(emptyMap(), emptyMap())

        assertEquals(before, state.viewport)
    }
}
