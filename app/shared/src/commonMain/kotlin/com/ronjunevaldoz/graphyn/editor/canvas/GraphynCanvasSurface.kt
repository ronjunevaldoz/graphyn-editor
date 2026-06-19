package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynCanvasBackdrop
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynConnectionLayer
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynConnectionMidpoints
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynEmptyCanvasHint
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynEmptyNodesHint
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynGroupLayer
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodePickerPopup
import com.ronjunevaldoz.graphyn.editor.canvas.components.compatiblePickerSpecs
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
fun GraphynCanvasSurface(
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    modifier: Modifier = Modifier,
    canvasCards: NodeCanvasRegistry? = null,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphynDs.colors.canvasBackground)
            .onSizeChanged { state.updateCanvasSize(it) }
            .graphicsLayer { clip = true }
            .focusRequester(focusRequester)
            .focusable()
            .graphynKeyboardShortcuts(state)
            .graphynDraftTrackingGesture(state)
            .graphynScrollZoomGesture(state),
        contentAlignment = Alignment.TopStart,
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(modifier = Modifier.fillMaxSize().testTag("canvas-background").graphynPanGesture(state, canvasCards))

        state.nodePickerState?.let { picker ->
            val wf = state.workflow
            if (wf != null) {
                GraphynNodePickerPopup(
                    screenPosition = picker.screenPosition,
                    compatibleSpecs = compatiblePickerSpecs(picker.draft, wf, nodeSpecs),
                    onPick = { spec, port ->
                        state.dispatch(GraphynEditorIntent.AddNodeAndConnect(spec, port, picker.worldPosition))
                    },
                    onDismiss = { state.dispatch(GraphynEditorIntent.DismissNodePicker) },
                )
            }
        }

        GraphynCanvasBackdrop(
            modifier = Modifier.fillMaxSize(),
            dotColor = GraphynDs.colors.border,
            viewport = state.viewport,
        )

        val workflow = state.workflow
        if (workflow == null) { GraphynEmptyCanvasHint(); return }
        if (workflow.nodes.isEmpty()) GraphynEmptyNodesHint()

        val outputColor = GraphynDs.colors.connectionLine
        val surfaceColor = GraphynDs.colors.surfaceCard

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = state.viewport.offset.x
                    translationY = state.viewport.offset.y
                    scaleX = state.viewport.scale
                    scaleY = state.viewport.scale
                },
        ) {
            GraphynGroupLayer(state)
            GraphynConnectionLayer(
                workflow = workflow, state = state, nodeSpecs = nodeSpecs,
                canvasCards = canvasCards,
                draft = state.connectionDraft, draftPointer = state.connectionDraftPosition,
                modifier = Modifier.fillMaxSize(), color = outputColor.copy(alpha = 0.6f),
            )
            GraphynConnectionMidpoints(
                workflow = workflow, state = state, nodeSpecs = nodeSpecs,
                canvasCards = canvasCards,
                connectionColor = outputColor, selectedColor = GraphynDs.colors.danger, surfaceColor = surfaceColor,
            )
            GraphynNodeLayer(
                workflow = workflow, state = state, nodeSpecs = nodeSpecs,
                canvasCards = canvasCards, surfaceColor = surfaceColor,
            )
        }

        state.rejectedConnectionPort?.let { (nodeId, portName) ->
            LaunchedEffect(nodeId, portName) { delay(2000); state.rejectedConnectionPort = null }
            TypeMismatchToast("Type mismatch on $portName")
        }
    }
}

@Composable
private fun TypeMismatchToast(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2D1111))
                .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            BasicText(message, style = TextStyle(color = Color(0xFFEF9A9A), fontSize = 11.sp))
        }
    }
}
