package com.ronjunevaldoz.graphyn.editor.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.shell.components.ArtifactItem
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynArtifactViewer
import com.ronjunevaldoz.graphyn.editor.shell.components.GraphynJobBadge
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
    var viewingArtifact by remember { mutableStateOf<ArtifactItem?>(null) }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().testTag("graphyn-canvas")) {
                canvasContent()
                GraphynZoomControls(
                    modifier = Modifier.align(Alignment.BottomStart),
                    onZoomIn = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomStep, Offset.Zero)) },
                    onZoomOut = { state.dispatch(GraphynEditorIntent.UpdateViewportTransform(Offset.Zero, ZoomOutStep, Offset.Zero)) },
                )
                GraphynJobBadge(
                    state = state,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
                val minimapWidth = 200.dp
                val minimapHeight = 130.dp
                GraphynMinimapDebugger(
                    state = state,
                    canvasCards = dependencies.canvasCards,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = minimapWidth, height = minimapHeight)
                        .testTag("minimap"),
                )
            }
            GraphynLogPanel(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                onArtifactClick = { viewingArtifact = it },
            )
        }

        val artifact = viewingArtifact
        if (artifact != null) {
            GraphynArtifactViewer(item = artifact, onDismiss = { viewingArtifact = null })
        }
    }
}
