package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynCanvasBackdrop
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynConnectionLayer
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynEmptyCanvasHint
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCard
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
fun GraphynCanvasSurface(
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp)
            .onSizeChanged { state.updateCanvasSize(it) }
            .graphicsLayer { clip = true }
            .pointerInput(state.connectionDraft?.fromNodeId, state.viewport) {
                if (state.connectionDraft == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        if (position != null) {
                            state.dispatch(
                                GraphynEditorIntent.UpdateConnectionDraftPosition(
                                    state.screenToWorld(position),
                                ),
                            )
                        }
                    }
                }
            }
            .pointerInput(state.viewport) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                        if (scrollDelta != Offset.Zero) {
                            val focus = event.changes.firstOrNull()?.position ?: Offset.Zero
                            val zoomFactor = (1f - scrollDelta.y * 0.0045f).coerceIn(0.7f, 1.35f)
                            state.dispatch(
                                GraphynEditorIntent.UpdateViewportTransform(
                                    pan = Offset.Zero,
                                    zoom = zoomFactor,
                                    focus = focus,
                                ),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.workflow, state.nodePositionsByNodeId) {
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val startWorld = state.screenToWorld(firstDown.position)
                        if (state.isWorldPositionOverNode(startWorld)) return@awaitEachGesture

                        var accumulatedDrag = Offset.Zero
                        var dragging = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (!change.pressed) break

                            val delta = change.position - change.previousPosition
                            if (delta != Offset.Zero) {
                                accumulatedDrag += delta
                                if (!dragging && accumulatedDrag.getDistance() <= viewConfiguration.touchSlop) {
                                    continue
                                }
                                dragging = true
                                change.consumePositionChange()
                                state.dispatch(
                                    GraphynEditorIntent.UpdateViewportTransform(
                                        pan = delta,
                                        zoom = 1f,
                                        focus = change.position,
                                    ),
                                )
                            }
                        }
                    }
                },
        )

        val dotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        GraphynCanvasBackdrop(
            modifier = Modifier.fillMaxSize(),
            dotColor = dotColor,
            viewport = state.viewport,
        )

        val workflow = state.workflow
        if (workflow == null) {
            GraphynEmptyCanvasHint()
            return
        }

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
            GraphynConnectionLayer(
                workflow = workflow,
                state = state,
                draft = state.connectionDraft,
                draftPointer = state.connectionDraftPosition,
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )

            workflow.nodes.forEachIndexed { index, node ->
                val spec = nodeSpecs.resolve(node.type)
                val position = state.nodePosition(node.id, index)
                GraphynNodeCard(
                    modifier = Modifier.offset { position },
                    node = node,
                    spec = spec,
                    selected = state.selectedNodeId == node.id,
                    outputs = state.outputsFor(node.id),
                    flattenedOutputs = state.flattenedOutputsFor(node.id),
                    onClick = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
                    onMove = { delta ->
                        state.dispatch(
                            GraphynEditorIntent.MoveNode(
                                nodeId = node.id,
                                delta = delta,
                            ),
                        )
                    },
                    onBeginConnection = { port ->
                        state.dispatch(GraphynEditorIntent.BeginConnection(node.id, port))
                    },
                    onCompleteConnection = { port ->
                        val draft = state.connectionDraft
                        val sourceNode = draft?.let { workflow.nodes.firstOrNull { nodeRef -> nodeRef.id == it.fromNodeId } }
                        val sourceSpec = sourceNode?.let { nodeSpecs.resolve(it.type) }
                        val sourcePort = draft?.let { draftConnection ->
                            sourceSpec?.outputs?.firstOrNull { it.name == draftConnection.fromPort }
                        }
                        val targetSpec = spec
                        val targetPort = targetSpec?.inputs?.firstOrNull { it.name == port }

                        if (draft == null || sourceNode == null || sourceSpec == null || sourcePort == null || targetPort == null) {
                            state.addDebugLog("Rejected connection: unknown port")
                            state.dispatch(GraphynEditorIntent.CancelConnection)
                        } else {
                            if (!WorkflowTypeCompatibility.isCompatible(targetPort.type, sourcePort.type)) {
                                state.addDebugLog(
                                    "Rejected connection ${draft.fromNodeId}:${draft.fromPort} -> ${node.id}:$port",
                                )
                                state.dispatch(GraphynEditorIntent.CancelConnection)
                            } else {
                                state.dispatch(GraphynEditorIntent.CompleteConnection(node.id, port))
                            }
                        }
                    },
                    isConnectingFrom = state.connectionDraft?.fromNodeId == node.id,
                )
            }
        }
    }
}
