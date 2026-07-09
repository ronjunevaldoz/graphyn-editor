package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.editor.canvas.components.PortCompatibility

internal fun GraphynEditorState.completeConnection(toNodeId: String, toPort: String) {
    val draft = connectionDraft ?: return
    val w = workflow ?: return

    val fromNodeType = w.nodes.firstOrNull { it.id == draft.fromNodeId }?.type
    val toNodeType = w.nodes.firstOrNull { it.id == toNodeId }?.type
    val fromSpec = fromNodeType?.let { nodeSpecs?.resolve(it) }
    val toSpec = toNodeType?.let { nodeSpecs?.resolve(it) }
    val outputPort = fromSpec?.outputs?.firstOrNull { it.name == draft.fromPort }
        ?: fromSpec?.inputs?.firstOrNull { it.name == draft.fromPort }
    val inputPort = toSpec?.inputs?.firstOrNull { it.name == toPort }
        ?: toSpec?.outputs?.firstOrNull { it.name == toPort }

    if (outputPort != null && inputPort != null) {
        if (!PortCompatibility.isCompatible(inputPort, outputPort)) {
            rejectConnectionPort(toNodeId, toPort)
            connectionDraft = null
            connectionDraftPosition = null
            log.push("Rejected connection: ${outputPort.type}[color=${outputPort.portColor}] → ${inputPort.type}[color=${inputPort.portColor}] incompatible")
            return
        }
    }

    val connection = if (draft.isFromInput) {
        ConnectionRef(fromNodeId = toNodeId, fromPort = toPort, toNodeId = draft.fromNodeId, toPort = draft.fromPort)
    } else {
        ConnectionRef(fromNodeId = draft.fromNodeId, fromPort = draft.fromPort, toNodeId = toNodeId, toPort = toPort)
    }
    workflow = w.copy(connections = upsertConnection(connection))
    connectionDraft = null
    connectionDraftPosition = null
    log.push("Connected ${connection.fromNodeId}:${connection.fromPort} -> ${connection.toNodeId}:${connection.toPort}")
}

internal fun GraphynEditorState.reconnectSelectedConnection(toNodeId: String, toPort: String) {
    val conn = selectedConnection ?: return
    val w = workflow ?: return
    val updated = conn.copy(toNodeId = toNodeId, toPort = toPort)
    workflow = w.copy(connections = upsertConnection(updated).filterNot { it == conn })
    selectedConnection = updated
    log.push("Reconnected ${conn.fromNodeId}:${conn.fromPort} -> $toNodeId:$toPort")
}
