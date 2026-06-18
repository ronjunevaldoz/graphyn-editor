package com.ronjunevaldoz.graphyn.editor.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.execution.NodeExecutionStatus
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

sealed interface GraphynEditorIntent {
    // Selection
    data class SelectNode(val nodeId: String?) : GraphynEditorIntent
    data class ToggleNodeSelection(val nodeId: String) : GraphynEditorIntent
    data object SelectAll : GraphynEditorIntent
    data object DeleteSelectedNode : GraphynEditorIntent
    data class SelectConnection(val connection: ConnectionRef?) : GraphynEditorIntent
    data object DeleteSelectedConnection : GraphynEditorIntent

    // Movement
    data class MoveNode(val nodeId: String, val delta: IntOffset) : GraphynEditorIntent
    data class MoveSelectedNodes(val delta: IntOffset) : GraphynEditorIntent

    // History
    data object Undo : GraphynEditorIntent
    data object Redo : GraphynEditorIntent

    // Clipboard
    data object CopySelection : GraphynEditorIntent
    data object Paste : GraphynEditorIntent
    data object DuplicateSelection : GraphynEditorIntent

    // Connections
    data class BeginConnection(val fromNodeId: String, val fromPort: String, val isFromInput: Boolean = false) : GraphynEditorIntent
    data class CompleteConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent
    data class UpdateConnectionDraftPosition(val position: Offset?) : GraphynEditorIntent
    data object CancelConnection : GraphynEditorIntent
    data class ReconnectSelectedConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent

    // Nodes
    data class AddNode(val spec: NodeSpec) : GraphynEditorIntent
    data class AddNodeAndConnect(val spec: NodeSpec, val toPort: String, val worldPosition: Offset) : GraphynEditorIntent
    data class UpdateNodeConfig(val nodeId: String, val key: String, val value: WorkflowValue) : GraphynEditorIntent

    // Viewport
    data class UpdateViewportTransform(val pan: Offset, val zoom: Float, val focus: Offset) : GraphynEditorIntent

    // Node picker
    data class ShowNodePicker(val screenPosition: Offset, val worldPosition: Offset) : GraphynEditorIntent
    data object DismissNodePicker : GraphynEditorIntent

    // Execution
    data class UpdateNodeExecutionStatus(val nodeId: String, val status: NodeExecutionStatus) : GraphynEditorIntent

    // Layout
    data object AutoLayout : GraphynEditorIntent
}
