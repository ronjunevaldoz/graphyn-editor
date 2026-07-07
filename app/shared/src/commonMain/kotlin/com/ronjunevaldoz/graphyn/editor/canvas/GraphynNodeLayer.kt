package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.canvas.components.GraphynNodePortDots
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.state.GraphynEditorState
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

@Composable
internal fun GraphynNodeLayer(
    workflow: WorkflowDefinition,
    state: GraphynEditorState,
    nodeSpecs: NodeSpecRegistry,
    canvasCards: NodeCanvasRegistry?,
    surfaceColor: Color,
    onEnterSubgraph: ((label: String, inner: WorkflowDefinition) -> Unit)? = null,
) {
    // Pass 1: annotation nodes (sticky notes, frames) always render behind regular nodes
    workflow.nodes.forEachIndexed { index, node ->
        val spec = nodeSpecs.resolve(node.type) ?: return@forEachIndexed
        val factory = canvasCards?.resolve(node.type) ?: return@forEachIndexed
        if (!factory.isAnnotation) return@forEachIndexed
        val position = state.nodePosition(node.id, index)
        val ctx = nodeCanvasContext(node, spec, state, onEnterSubgraph)
        Box(modifier = Modifier.offset { position }) {
            with(factory) { NodeCanvas(ctx) }
        }
    }

    // Pass 2: regular nodes on top. Every node resolves to a factory — a host-registered card,
    // a subgraph card, or a default FieldCardFactory sized from its spec.
    workflow.nodes.forEachIndexed { index, node ->
        val spec = resolveRenderSpec(node, nodeSpecs)
        val position = state.nodePosition(node.id, index)
        val factory = resolveNodeFactory(node, canvasCards, nodeSpecs)
            ?: FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
        if (factory.isAnnotation) return@forEachIndexed

        val ctx = nodeCanvasContext(node, spec, state, onEnterSubgraph)
        Box(modifier = Modifier.offset { position }) {
            with(factory) { NodeCanvas(ctx) }
        }

        val inputAnchorYs = spec.inputs.mapIndexed { i, _ -> factory.portAnchorY(i, true, spec) }
        val outputAnchorYs = spec.outputs.mapIndexed { i, _ -> factory.portAnchorY(i, false, spec) }
        GraphynNodePortDots(
            node = node, position = position, spec = spec,
            inputAnchorYs = inputAnchorYs,
            outputAnchorYs = outputAnchorYs,
            nodeWidthDp = factory.nodeWidth,
            state = state, workflow = workflow, nodeSpecs = nodeSpecs,
            surfaceColor = surfaceColor,
        )
    }
}

@Composable
private fun nodeCanvasContext(
    node: NodeRef,
    spec: NodeSpec,
    state: GraphynEditorState,
    onEnterSubgraph: ((label: String, inner: WorkflowDefinition) -> Unit)?,
): NodeCanvasContext = NodeCanvasContext(
    node = node,
    spec = spec,
    selected = state.selectedNodeId == node.id,
    executionStatus = state.executionStatusByNodeId[node.id] ?: NodeExecutionStatus.Idle,
    onSelect = { state.dispatch(GraphynEditorIntent.SelectNode(node.id)) },
    onMove = { delta -> state.dispatch(GraphynEditorIntent.MoveNode(nodeId = node.id, delta = delta)) },
    onConfigChange = { key, value -> state.dispatch(GraphynEditorIntent.UpdateNodeConfig(node.id, key, value)) },
    contentColor = GraphynDs.colors.textPrimary,
    onEnterSubgraph = node.subgraph?.let { sg -> onEnterSubgraph?.let { cb -> { cb(spec.label, sg) } } },
    executionOutputs = state.outputsFor(node.id),
)
