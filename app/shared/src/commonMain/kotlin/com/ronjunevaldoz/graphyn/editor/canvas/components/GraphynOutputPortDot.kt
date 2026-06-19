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
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility
import com.ronjunevaldoz.graphyn.core.model.displayName
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynOutputPortDot(
    node: NodeRef,
    portIndex: Int,
    outputPort: PortSpec,
    position: IntOffset,
    anchorYDp: Int,
    nodeWidthDp: Int,
    draft: GraphynConnectionDraft?,
    workflow: WorkflowDefinition,
    nodeSpecs: NodeSpecRegistry,
    state: GraphynEditorState,
    surfaceColor: Color,
) {
    val outputColor = outputPort.portColor()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isInputDraftTarget = draft != null && draft.isFromInput && run {
        val draftNode = workflow.nodes.firstOrNull { it.id == draft.fromNodeId }
        val inputPort = draftNode?.let { nodeSpecs.resolve(it.type) }
            ?.inputs?.firstOrNull { it.name == draft.fromPort }
        inputPort != null && WorkflowTypeCompatibility.isCompatible(inputPort.type, outputPort.type)
    }
    Box(
        modifier = Modifier
            .testTag("output-port-${node.id}-${outputPort.name}")
            .hoverable(interactionSource)
            .offset {
                val nodeWidthPx = nodeWidthDp.dp.roundToPx()
                val dotRadiusPx = GraphynCanvasMetrics.PortDotRadius.dp.roundToPx()
                IntOffset(
                    x = position.x + nodeWidthPx - dotRadiusPx,
                    y = position.y + anchorYDp.dp.roundToPx() - dotRadiusPx,
                )
            }
            .size(GraphynCanvasMetrics.PortDotDiameter.dp)
            .clip(CircleShape)
            .background(
                when {
                    isInputDraftTarget && isHovered -> outputColor.copy(alpha = 0.25f)
                    isHovered -> outputColor.copy(alpha = 0.2f)
                    else -> surfaceColor
                },
            )
            .border(if (isHovered || isInputDraftTarget) 3.dp else 2.dp, outputColor, CircleShape)
            .clickable {
                when {
                    draft != null && draft.isFromInput -> {
                        val draftNode = workflow.nodes.firstOrNull { it.id == draft.fromNodeId }
                        val inputPort = draftNode?.let { nodeSpecs.resolve(it.type) }
                            ?.inputs?.firstOrNull { it.name == draft.fromPort }
                        if (inputPort == null || !WorkflowTypeCompatibility.isCompatible(inputPort.type, outputPort.type)) {
                            state.rejectedConnectionPort = node.id to outputPort.name
                            state.addDebugLog("Rejected: ${node.id}:${outputPort.name} → ${draft.fromNodeId}:${draft.fromPort} (type mismatch)")
                            state.dispatch(GraphynEditorIntent.CancelConnection)
                        } else {
                            state.rejectedConnectionPort = null
                            state.dispatch(GraphynEditorIntent.CompleteConnection(node.id, outputPort.name))
                        }
                    }
                    draft != null -> {
                        state.dispatch(GraphynEditorIntent.CancelConnection)
                        state.dispatch(GraphynEditorIntent.BeginConnection(node.id, outputPort.name))
                    }
                    else -> state.dispatch(GraphynEditorIntent.BeginConnection(node.id, outputPort.name))
                }
            },
    ) {
        if (isHovered && draft == null) {
            PortTooltip("← ${outputPort.name}: ${outputPort.type.displayName()}", outputColor)
        }
    }
}
