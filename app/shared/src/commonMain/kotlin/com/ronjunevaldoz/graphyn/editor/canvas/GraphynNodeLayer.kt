package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodeCard
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
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
    canvasCards: NodeCanvasRegistry?,
    surfaceColor: Color,
) {
    // Pass 1: annotation nodes (sticky notes, frames) always render behind regular nodes
    workflow.nodes.forEachIndexed { index, node ->
        val spec = nodeSpecs.resolve(node.type) ?: return@forEachIndexed
        val factory = canvasCards?.resolve(node.type) ?: return@forEachIndexed
        if (!factory.isAnnotation) return@forEachIndexed
        val position = state.nodePosition(node.id, index)
        val ctx = NodeCanvasContext(
            node = node, spec = spec,
            selected = state.selectedNodeId == node.id,
            executionStatus = state.executionStatusByNodeId[node.id] ?: NodeExecutionStatus.Idle,
            onSelect = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
            onMove = { delta -> state.dispatch(GraphynEditorIntent.MoveNode(nodeId = node.id, delta = delta)) },
            onConfigChange = { key, value -> state.dispatch(GraphynEditorIntent.UpdateNodeConfig(node.id, key, value)) },
            contentColor = GraphynDs.colors.textPrimary,
        )
        Box(modifier = Modifier.offset { position }) {
            with(factory) { NodeCanvas(ctx) }
        }
    }

    // Pass 2: regular nodes on top
    workflow.nodes.forEachIndexed { index, node ->
        val spec = nodeSpecs.resolve(node.type)
        val position = state.nodePosition(node.id, index)
        val factory = spec?.let { canvasCards?.resolve(node.type) }
        if (factory?.isAnnotation == true) return@forEachIndexed

        if (factory != null) {
            val ctx = NodeCanvasContext(
                node = node,
                spec = spec,
                selected = state.selectedNodeId == node.id,
                executionStatus = state.executionStatusByNodeId[node.id] ?: NodeExecutionStatus.Idle,
                onSelect = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
                onMove = { delta -> state.dispatch(GraphynEditorIntent.MoveNode(nodeId = node.id, delta = delta)) },
                onConfigChange = { key, value -> state.dispatch(GraphynEditorIntent.UpdateNodeConfig(node.id, key, value)) },
                contentColor = GraphynDs.colors.textPrimary,
            )
            Box(modifier = Modifier.offset { position }) {
                with(factory) { NodeCanvas(ctx) }
            }
        } else {
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
        }

        if (spec != null) {
            val nodeWidthDp = factory?.nodeWidth ?: GraphynCanvasMetrics.NodeSize.width
            val inputAnchorYs = spec.inputs.mapIndexed { i, _ ->
                factory?.portAnchorY(i, true, spec) ?: GraphynCanvasMetrics.portAnchorY(i)
            }
            val outputAnchorYs = spec.outputs.mapIndexed { i, _ ->
                factory?.portAnchorY(i, false, spec) ?: GraphynCanvasMetrics.portAnchorY(i)
            }
            GraphynNodePortDots(
                node = node, position = position, spec = spec,
                inputAnchorYs = inputAnchorYs,
                outputAnchorYs = outputAnchorYs,
                nodeWidthDp = nodeWidthDp,
                state = state, workflow = workflow, nodeSpecs = nodeSpecs,
                surfaceColor = surfaceColor,
            )
        }
    }
}
