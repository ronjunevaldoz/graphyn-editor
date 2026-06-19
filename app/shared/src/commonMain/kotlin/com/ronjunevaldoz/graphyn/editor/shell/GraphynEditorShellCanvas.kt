package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynLogPanel
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynMinimapDebugger
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynZoomControls
import com.ronjunevaldoz.graphyn.editor.shell.components.ZoomOutStep
import com.ronjunevaldoz.graphyn.editor.shell.components.ZoomStep
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynEditorShellCanvas(
    state: GraphynEditorState,
    dependencies: GraphynEditorShellDependencies,
    modifier: Modifier = Modifier,
    canvasContent: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().testTag("graphyn-canvas")) {
            canvasContent()
            GraphynZoomControls(
                modifier = Modifier.align(Alignment.BottomStart),
                onZoomIn = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomStep, Offset.Zero)) },
                onZoomOut = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomOutStep, Offset.Zero)) },
            )
            val minimapWidth = 200.dp
            val cs = state.canvasSize
            val minimapHeight = if (cs.width > 0)
                (minimapWidth.value * cs.height.toFloat() / cs.width.toFloat()).coerceIn(80f, 200f).dp
            else 130.dp
            GraphynMinimapDebugger(
                state = state,
                canvasCards = dependencies.canvasCards,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = minimapWidth, height = minimapHeight)
                    .testTag("minimap")
                    .graphicsLayer { alpha = 0.9f },
            )
        }
        GraphynLogPanel(modifier = Modifier.fillMaxWidth(), state = state)
    }
}
