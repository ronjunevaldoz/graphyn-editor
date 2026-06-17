package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef

internal fun GraphynEditorState.selectNode(nodeId: String?) {
    selectedNodeId = nodeId
    selectedConnection = null
}

internal fun GraphynEditorState.selectConnection(connection: ConnectionRef?) {
    selectedConnection = connection
    selectedNodeId = null
}

internal fun GraphynEditorState.deleteSelectedNode() {
    val nodeId = selectedNodeId ?: return
    workflow = workflow?.copy(
        nodes = workflow?.nodes.orEmpty().filterNot { it.id == nodeId },
        connections = workflow?.connections.orEmpty().filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId },
    )
    layout.removeNode(nodeId)
    nodeOutputsByNodeId = nodeOutputsByNodeId - nodeId
    selectedNodeId = null
    if (selectedConnection?.fromNodeId == nodeId || selectedConnection?.toNodeId == nodeId) {
        selectedConnection = null
    }
    log.push("Deleted node $nodeId")
}

internal fun GraphynEditorState.deleteSelectedConnection() {
    val conn = selectedConnection ?: return
    workflow = workflow?.copy(connections = workflow?.connections.orEmpty().filterNot { it == conn })
    selectedConnection = null
    log.push("Deleted connection ${conn.fromNodeId}:${conn.fromPort} -> ${conn.toNodeId}:${conn.toPort}")
}
