package com.ronjunevaldoz.graphyn.editor.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynInputPortDot(
    node: NodeRef,
    portIndex: Int,
    inputPort: PortSpec,
    position: IntOffset,
    draft: GraphynConnectionDraft?,
    selectedConn: ConnectionRef?,
    workflow: WorkflowDefinition,
    nodeSpecs: NodeSpecRegistry,
    state: GraphynEditorState,
    surfaceColor: Color,
) {
    val inputColor = inputPort.type.portColor()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isCompatible = draft != null && !draft.isFromInput && run {
        val srcPort = workflow.nodes.firstOrNull { it.id == draft.fromNodeId }
            ?.let { nodeSpecs.resolve(it.type) }?.outputs?.firstOrNull { it.name == draft.fromPort }
        val tgtPort = nodeSpecs.resolve(node.type)?.inputs?.firstOrNull { it.name == inputPort.name }
        srcPort != null && tgtPort != null && WorkflowTypeCompatibility.isCompatible(tgtPort.type, srcPort.type)
    }
    val isReconnectTarget = selectedConn != null && draft == null
    Box(
        modifier = Modifier
            .testTag("input-port-${node.id}-${inputPort.name}")
            .hoverable(interactionSource)
            .offset {
                val dotRadiusPx = GraphynCanvasMetrics.PortDotRadius.dp.roundToPx()
                IntOffset(
                    x = position.x - dotRadiusPx,
                    y = position.y + GraphynCanvasMetrics.portAnchorY(portIndex).dp.roundToPx() - dotRadiusPx,
                )
            }
            .size(GraphynCanvasMetrics.PortDotDiameter.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompatible && isHovered -> inputColor.copy(alpha = 0.25f)
                    isReconnectTarget && isHovered -> inputColor.copy(alpha = 0.15f)
                    else -> surfaceColor
                },
            )
            .border(
                width = if (isHovered) 3.dp else 2.dp,
                color = if (isReconnectTarget) inputColor.copy(alpha = 0.6f) else inputColor,
                shape = CircleShape,
            )
            .clickable {
                when {
                    draft != null && !draft.isFromInput -> {
                        val srcPort = workflow.nodes.firstOrNull { it.id == draft.fromNodeId }
                            ?.let { nodeSpecs.resolve(it.type) }?.outputs?.firstOrNull { it.name == draft.fromPort }
                        val tgtPort = nodeSpecs.resolve(node.type)?.inputs?.firstOrNull { it.name == inputPort.name }
                        if (srcPort == null || tgtPort == null || !WorkflowTypeCompatibility.isCompatible(tgtPort.type, srcPort.type)) {
                            state.addDebugLog("Rejected: ${draft.fromNodeId}:${draft.fromPort} -> ${node.id}:${inputPort.name}")
                            state.dispatch(GraphynEditorIntent.CancelConnection)
                        } else {
                            state.dispatch(GraphynEditorIntent.CompleteConnection(node.id, inputPort.name))
                        }
                    }
                    draft != null && draft.isFromInput -> {
                        state.dispatch(GraphynEditorIntent.CancelConnection)
                        state.dispatch(GraphynEditorIntent.BeginConnection(node.id, inputPort.name, isFromInput = true))
                    }
                    selectedConn != null ->
                        state.dispatch(GraphynEditorIntent.ReconnectSelectedConnection(node.id, inputPort.name))
                    else ->
                        state.dispatch(GraphynEditorIntent.BeginConnection(node.id, inputPort.name, isFromInput = true))
                }
            },
    )
}
