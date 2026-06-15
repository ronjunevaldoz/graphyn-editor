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
            viewport = GraphynViewport(offset = Offset(-700f, 0f), scale = 2f)
        }

        rule.setContent {
            GraphynEditorShell(
                dependencies = GraphynEditorShellDependencies(
                    nodeSpecs = DefaultNodeSpecRegistry(),
                ),
                state = state,
                appearanceState = rememberGraphynAppearanceState(),
            )
        }

        rule.waitForIdle()

        val minimapImage = rule.onNodeWithTag("minimap").captureToImage()
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

        val centerY = viewportRect.center.y.toInt().coerceIn(0, pixels.height - 1)
        val outsideX = (viewportRect.left.toInt() - 3).coerceIn(0, pixels.width - 1)
        val outside = pixels[outsideX, centerY]

        val borderX = viewportRect.left.toInt().coerceIn(0, pixels.width - 1)
        val borderY = centerY
        val border = pixels[borderX, borderY]
        assertNotEquals(outside, border, "Viewport border should be visible and distinct from the outside area")
    }
}
