package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.editor.interaction.GraphynConnectionDraft
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynEditorIntent
import com.ronjunevaldoz.graphyn.editor.interaction.GraphynNodePickerState

internal fun GraphynEditorState.handleDispatch(intent: GraphynEditorIntent) {
    when (intent) {
        is GraphynEditorIntent.SelectNode -> selectNode(intent.nodeId)
        is GraphynEditorIntent.ToggleNodeSelection -> toggleNodeSelection(intent.nodeId)
        GraphynEditorIntent.SelectAll -> selectAllNodes()
        GraphynEditorIntent.DeleteSelectedNode -> withHistory { deleteSelectedNode() }
        is GraphynEditorIntent.SelectConnection -> selectConnection(intent.connection)
        GraphynEditorIntent.DeleteSelectedConnection -> withHistory { deleteSelectedConnection() }
        is GraphynEditorIntent.MoveNode -> moveNode(intent.nodeId, intent.delta)
        is GraphynEditorIntent.MoveSelectedNodes -> moveSelectedNodes(intent.delta)
        GraphynEditorIntent.Undo -> restoreSnapshot(history.undo(snapshot()))
        GraphynEditorIntent.Redo -> restoreSnapshot(history.redo(snapshot()))
        GraphynEditorIntent.CopySelection -> copySelection()
        GraphynEditorIntent.Paste -> withHistory { pasteNodes() }
        GraphynEditorIntent.DuplicateSelection -> withHistory { duplicateSelection() }
        is GraphynEditorIntent.BeginConnection -> {
            connectionDraft = GraphynConnectionDraft(intent.fromNodeId, intent.fromPort, intent.isFromInput)
            connectionDraftPosition = null
        }
        is GraphynEditorIntent.CompleteConnection -> withHistory { completeConnection(intent.toNodeId, intent.toPort) }
        is GraphynEditorIntent.AddNode -> withHistory { addNode(intent.spec) }
        is GraphynEditorIntent.AddNodeAndConnect -> withHistory { addNodeAndConnect(intent.spec, intent.toPort, intent.worldPosition) }
        is GraphynEditorIntent.UpdateConnectionDraftPosition -> {
            if (nodePickerState == null) connectionDraftPosition = intent.position
        }
        is GraphynEditorIntent.UpdateViewportTransform -> updateViewportTransform(intent.pan, intent.zoom, intent.focus)
        GraphynEditorIntent.CancelConnection -> {
            connectionDraft = null; connectionDraftPosition = null; nodePickerState = null
        }
        is GraphynEditorIntent.ReconnectSelectedConnection -> withHistory { reconnectSelectedConnection(intent.toNodeId, intent.toPort) }
        is GraphynEditorIntent.ShowNodePicker -> {
            val draft = connectionDraft ?: return
            connectionDraftPosition = intent.worldPosition
            nodePickerState = GraphynNodePickerState(intent.screenPosition, intent.worldPosition, draft)
        }
        GraphynEditorIntent.DismissNodePicker -> {
            nodePickerState = null; connectionDraft = null; connectionDraftPosition = null
        }
        is GraphynEditorIntent.UpdateNodeExecutionStatus ->
            executionStatusByNodeId = executionStatusByNodeId + (intent.nodeId to intent.status)
        GraphynEditorIntent.AutoLayout -> {
            val result = performAutoLayout() ?: return
            fitToContent(positions = result.positions, sizes = result.sizes)
        }
        GraphynEditorIntent.AutoLayoutBfs -> {
            val result = performAutoLayout(minimizeCrossings = false) ?: return
            fitToContent(positions = result.positions, sizes = result.sizes)
        }
        is GraphynEditorIntent.UpdateNodeConfig -> withHistory { updateNodeConfig(intent.nodeId, intent.key, intent.value) }
        GraphynEditorIntent.CreateGroupFromSelection -> createGroupFromSelection()
        is GraphynEditorIntent.DeleteGroup -> groups = groups.filterNot { it.id == intent.groupId }
        is GraphynEditorIntent.RenameGroup -> groups = groups.map { if (it.id == intent.groupId) it.copy(label = intent.label) else it }
        GraphynEditorIntent.CollapseSelectionToSubgraph -> withHistory { collapseSelectionToSubgraph() }
        is GraphynEditorIntent.ExpandSubgraph -> withHistory { expandSubgraphNode(intent.nodeId) }
    }
}

private fun GraphynEditorState.createGroupFromSelection() {
    val ids = effectiveSelectedNodeIds
    if (ids.size < 2) return
    groups = groups + NodeGroup(nodeIds = ids)
}
