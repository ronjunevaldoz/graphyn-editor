package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

internal fun GraphynEditorState.addNode(spec: NodeSpec) {
    val current = workflow ?: WorkflowDefinition("workflow", "Untitled Workflow", emptyList(), emptyList())
    val nodeId = buildNodeId(spec, current.nodes)
    val next = current.copy(nodes = current.nodes + NodeRef(nodeId, spec.type, spec.defaultValues))
    workflow = next
    selectedNodeId = nodeId
    connectionDraft = null
    connectionDraftPosition = null
    val center = screenToWorld(Offset(canvasSize.width / 2f, canvasSize.height / 2f))
    layout.setNodePosition(nodeId, IntOffset(center.x.toInt(), center.y.toInt()))
    log.push("Added node $nodeId (${spec.label})")
}

internal fun GraphynEditorState.addNodeAndConnect(spec: NodeSpec, toPort: String, worldPosition: Offset) {
    val draft = connectionDraft ?: return
    val current = workflow ?: WorkflowDefinition("workflow", "Untitled Workflow", emptyList(), emptyList())
    val nodeId = buildNodeId(spec, current.nodes)
    val node = NodeRef(nodeId, spec.type, spec.defaultValues)
    val connection = if (draft.isFromInput) {
        ConnectionRef(fromNodeId = nodeId, fromPort = toPort, toNodeId = draft.fromNodeId, toPort = draft.fromPort)
    } else {
        ConnectionRef(fromNodeId = draft.fromNodeId, fromPort = draft.fromPort, toNodeId = nodeId, toPort = toPort)
    }
    workflow = current.copy(nodes = current.nodes + node, connections = current.connections + connection)
    connectionDraft = null
    connectionDraftPosition = null
    nodePickerState = null
    selectedNodeId = nodeId
    layout.setNodePosition(nodeId, IntOffset(worldPosition.x.toInt(), worldPosition.y.toInt()))
    log.push("Added $nodeId and connected ${connection.fromNodeId}:${connection.fromPort} -> ${connection.toNodeId}:${connection.toPort}")
}

internal fun GraphynEditorState.updateNodeConfig(nodeId: String, key: String, value: com.ronjunevaldoz.graphyn.core.model.WorkflowValue) {
    val wf = workflow ?: return
    workflow = wf.copy(
        nodes = wf.nodes.map { node ->
            if (node.id == nodeId) node.copy(config = node.config + (key to value)) else node
        },
    )
}
