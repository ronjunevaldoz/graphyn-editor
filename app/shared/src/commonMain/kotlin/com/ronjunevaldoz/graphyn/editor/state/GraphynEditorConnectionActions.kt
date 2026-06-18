package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowTypeCompatibility

internal fun GraphynEditorState.completeConnection(toNodeId: String, toPort: String) {
    val draft = connectionDraft ?: return
    val w = workflow ?: return

    val fromNodeType = w.nodes.firstOrNull { it.id == draft.fromNodeId }?.type
    val toNodeType = w.nodes.firstOrNull { it.id == toNodeId }?.type
    val fromSpec = fromNodeType?.let { nodeSpecs?.resolve(it) }
    val toSpec = toNodeType?.let { nodeSpecs?.resolve(it) }
    val outputType = fromSpec?.outputs?.firstOrNull { it.name == draft.fromPort }?.type
        ?: fromSpec?.inputs?.firstOrNull { it.name == draft.fromPort }?.type
    val inputType = toSpec?.inputs?.firstOrNull { it.name == toPort }?.type
        ?: toSpec?.outputs?.firstOrNull { it.name == toPort }?.type

    if (outputType != null && inputType != null) {
        val compatible = outputType is WorkflowType.OpaqueType || inputType is WorkflowType.OpaqueType
            || WorkflowTypeCompatibility.isCompatible(inputType, outputType)
        if (!compatible) {
            rejectedConnectionPort = toNodeId to toPort
            connectionDraft = null
            connectionDraftPosition = null
            log.push("Rejected connection: $outputType is not compatible with $inputType")
            return
        }
    }

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
