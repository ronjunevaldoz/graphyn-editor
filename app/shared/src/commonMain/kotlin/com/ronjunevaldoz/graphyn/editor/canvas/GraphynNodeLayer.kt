package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCard
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardFooter
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardHeader
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardPorts
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCardSlots
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodePortDots
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState

@Composable
internal fun GraphynNodeLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    outputColor: Color,
    inputColor: Color,
    surfaceColor: Color,
) {
    workflow.nodes.forEachIndexed { index, node ->
        val spec = nodeSpecs.resolve(node.type)
        val position = state.nodePosition(node.id, index)
        GraphynNodeCard(
            modifier = Modifier.offset { position },
            selected = state.selectedNodeId == node.id,
            executionStatus = state.executionStatusByNodeId[node.id] ?: NodeExecutionStatus.Idle,
            onClick = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
            onMove = { delta -> state.dispatch(GraphynEditorIntent.MoveNode(nodeId = node.id, delta = delta)) },
            slots = GraphynNodeCardSlots(
                header = { GraphynNodeCardHeader(node = node, spec = spec) },
                ports = { GraphynNodeCardPorts(spec = spec) },
                footer = {
                    GraphynNodeCardFooter(
                        outputs = state.outputsFor(node.id),
                        flattenedOutputs = state.flattenedOutputsFor(node.id),
                        isConnectingFrom = state.connectionDraft?.fromNodeId == node.id,
                    )
                },
            ),
        )
        if (spec != null) {
            GraphynNodePortDots(
                node = node,
                position = position,
                spec = spec,
                state = state,
                workflow = workflow,
                nodeSpecs = nodeSpecs,
                outputColor = outputColor,
                inputColor = inputColor,
                surfaceColor = surfaceColor,
            )
        }
    }
}
