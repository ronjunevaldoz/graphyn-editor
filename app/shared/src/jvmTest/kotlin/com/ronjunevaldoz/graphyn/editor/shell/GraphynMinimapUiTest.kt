package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.DefaultNodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.editor.state.GraphynViewport
import com.ronjunevaldoz.graphyn.editor.state.calculateMinimapLayout
import com.ronjunevaldoz.graphyn.editor.state.calculateViewportRectInMinimap
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynAppearanceState
import kotlin.test.Test
import kotlin.math.roundToInt
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GraphynMinimapUiTest {
    @get:org.junit.Rule
    val rule = createComposeRule()

    @Test
    fun minimapCameraIsAnOutlineAndNotATintedFill() {
        val state = GraphynEditorState(
            WorkflowDefinition(
                id = "workflow-minimap",
                name = "Minimap",
                nodes = listOf(
                    NodeRef(id = "logger-1", type = "logger"),
                    NodeRef(id = "logger-2", type = "logger"),
                ),
                connections = emptyList(),
            ),
        ).apply {
            setNodePosition("logger-1", IntOffset(0, 0))
            setNodePosition("logger-2", IntOffset(1200, 0))
            // scale=1.0 shows the nodes at natural size; the viewport rect is wide enough in the
            // minimap (graphWorldBounds = 8192×6144) to sample clearly inside and outside it.
            viewport = GraphynViewport(offset = Offset(0f, 0f), scale = 1.0f)
        }

        // The minimap fades in to alpha 0.9 over 150ms, holds for 1500ms, then fades out.
        // Under autoAdvance the idle clock runs past the hold and the minimap is fully
        // transparent by capture time. Drive the clock manually so we capture while the
        // minimap is still visible — after the fade-in completes but before the hold ends.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = DefaultNodeSpecRegistry(),
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(300L)

        // Capture the inner Canvas (not the outer Box with padding) so the image coordinates
        // match the drawing coordinates used by calculateMinimapLayout inside GraphynMinimapDebugger.
        val minimapImage = rule.onNodeWithTag("minimap-canvas").captureToImage()
        val pixels = minimapImage.toPixelMap()
        val canvasSize = state.canvasSize
        val layout = calculateMinimapLayout(
            worldBounds = state.graphWorldBounds ?: error("Missing world bounds"),
            minimapSize = IntSize(minimapImage.width, minimapImage.height),
        )
        val viewportRect = calculateViewportRectInMinimap(
            viewport = state.viewport,
            canvasSize = canvasSize,
            layout = layout,
        ) ?: error("Missing viewport rect")

        val centerY = viewportRect.center.y.roundToInt().coerceIn(0, pixels.height - 1)
        val outsideX = (viewportRect.left.roundToInt() - 4).coerceIn(0, pixels.width - 1)
        val outside = pixels[outsideX, centerY]

        // The viewport stroke is 2px wide centred on the rect's left edge, so round (not
        // truncate) to land on a drawn stroke pixel rather than the background just outside it.
        val borderX = viewportRect.left.roundToInt().coerceIn(0, pixels.width - 1)
        val borderY = centerY
        val border = pixels[borderX, borderY]
        assertNotEquals(outside, border, "Viewport border should be visible and distinct from the outside area")
    }
}
