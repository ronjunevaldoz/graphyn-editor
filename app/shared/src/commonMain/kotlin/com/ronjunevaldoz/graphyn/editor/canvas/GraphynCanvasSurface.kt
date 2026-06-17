package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynCanvasBackdrop
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynConnectionLayer
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynEmptyCanvasHint
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardFooter
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardHeader
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardSlots
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardPorts
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
                                change.consume()
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
                nodeSpecs = nodeSpecs,
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
                    selected = state.selectedNodeId == node.id,
                    onClick = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
                    onMove = { delta ->
                        state.dispatch(
                            GraphynEditorIntent.MoveNode(
                                nodeId = node.id,
                                delta = delta,
                            ),
                        )
                    },
                    slots = GraphynNodeCardSlots(
                        header = {
                            GraphynNodeCardHeader(
                                node = node,
                                spec = spec,
                            )
                        },
                        ports = {
                            GraphynNodeCardPorts(spec = spec)
                        },
                        footer = {
                            GraphynNodeCardFooter(
                                outputs = state.outputsFor(node.id),
                                flattenedOutputs = state.flattenedOutputsFor(node.id),
                                isConnectingFrom = state.connectionDraft?.fromNodeId == node.id,
                            )
                        },
                    ),
                )
            }

            // Edge port dots — rendered outside card clip so they straddle the card border
            workflow.nodes.forEachIndexed { index, node ->
                val spec = nodeSpecs.resolve(node.type) ?: return@forEachIndexed
                val position = state.nodePosition(node.id, index)
                val outputColor = MaterialTheme.colorScheme.primary
                val inputColor = MaterialTheme.colorScheme.secondary
                val surfaceColor = MaterialTheme.colorScheme.surface

                spec.outputs.forEachIndexed { portIndex, outputPort ->
                    val anchorY = GraphynCanvasMetrics.portAnchorY(portIndex)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = position.x + GraphynCanvasMetrics.NodeSize.width - GraphynCanvasMetrics.PortDotRadius,
                                    y = position.y + anchorY - GraphynCanvasMetrics.PortDotRadius,
                                )
                            }
                            .size(GraphynCanvasMetrics.PortDotDiameter.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(2.dp, outputColor, CircleShape)
                            .clickable {
                                state.dispatch(GraphynEditorIntent.BeginConnection(node.id, outputPort.name))
                            },
                    )
                }

                spec.inputs.forEachIndexed { portIndex, inputPort ->
                    val anchorY = GraphynCanvasMetrics.portAnchorY(portIndex)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = position.x - GraphynCanvasMetrics.PortDotRadius,
                                    y = position.y + anchorY - GraphynCanvasMetrics.PortDotRadius,
                                )
                            }
                            .size(GraphynCanvasMetrics.PortDotDiameter.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(2.dp, inputColor, CircleShape)
                            .clickable {
                                val draft = state.connectionDraft ?: return@clickable
                                val sourceNode = workflow.nodes.firstOrNull { it.id == draft.fromNodeId }
                                val sourceSpec = sourceNode?.let { nodeSpecs.resolve(it.type) }
                                val sourcePort = sourceSpec?.outputs?.firstOrNull { it.name == draft.fromPort }
                                val targetPort = spec.inputs.firstOrNull { it.name == inputPort.name }

                                if (sourcePort == null || targetPort == null) {
                                    state.addDebugLog("Rejected connection: unknown port")
                                    state.dispatch(GraphynEditorIntent.CancelConnection)
                                } else if (!WorkflowTypeCompatibility.isCompatible(targetPort.type, sourcePort.type)) {
                                    state.addDebugLog("Rejected connection ${draft.fromNodeId}:${draft.fromPort} -> ${node.id}:${inputPort.name}")
                                    state.dispatch(GraphynEditorIntent.CancelConnection)
                                } else {
                                    state.dispatch(GraphynEditorIntent.CompleteConnection(node.id, inputPort.name))
                                }
                            },
                    )
                }
            }
        }
    }
}
