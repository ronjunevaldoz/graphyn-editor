package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.listElementType

internal fun GraphynEditorState.upsertConnection(connection: ConnectionRef): List<ConnectionRef> {
    val current = workflow?.connections.orEmpty()
    return if (allowsMultipleIncomingConnections(connection.toNodeId, connection.toPort)) {
        current + connection
    } else {
        current.filterNot { it.toNodeId == connection.toNodeId && it.toPort == connection.toPort } + connection
    }
}

internal fun GraphynEditorState.allowsMultipleIncomingConnections(nodeId: String, portName: String): Boolean {
    val workflow = workflow ?: return false
    val nodeType = workflow.nodes.firstOrNull { it.id == nodeId }?.type ?: return false
    val spec = nodeSpecs?.resolve(nodeType) ?: return true
    val input = spec.inputs.firstOrNull { it.name == portName } ?: return true
    return input.type.listElementType() != null
}
