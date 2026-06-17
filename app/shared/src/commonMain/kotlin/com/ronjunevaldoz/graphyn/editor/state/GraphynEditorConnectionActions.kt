package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef

internal fun GraphynEditorState.completeConnection(toNodeId: String, toPort: String) {
    val draft = connectionDraft ?: return
    val w = workflow ?: return
    val connection = if (draft.isFromInput) {
        ConnectionRef(fromNodeId = toNodeId, fromPort = toPort, toNodeId = draft.fromNodeId, toPort = draft.fromPort)
    } else {
        ConnectionRef(fromNodeId = draft.fromNodeId, fromPort = draft.fromPort, toNodeId = toNodeId, toPort = toPort)
    }
    workflow = w.copy(connections = w.connections + connection)
    connectionDraft = null
    connectionDraftPosition = null
    log.push("Connected ${connection.fromNodeId}:${connection.fromPort} -> ${connection.toNodeId}:${connection.toPort}")
}

internal fun GraphynEditorState.reconnectSelectedConnection(toNodeId: String, toPort: String) {
    val conn = selectedConnection ?: return
    val w = workflow ?: return
    val updated = conn.copy(toNodeId = toNodeId, toPort = toPort)
    workflow = w.copy(connections = w.connections.map { if (it == conn) updated else it })
    selectedConnection = updated
    log.push("Reconnected ${conn.fromNodeId}:${conn.fromPort} -> $toNodeId:$toPort")
}
