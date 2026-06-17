package com.ronjunevaldoz.graphyn.editor.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec

sealed interface GraphynEditorIntent {
    data class SelectNode(val nodeId: String?) : GraphynEditorIntent
    data object DeleteSelectedNode : GraphynEditorIntent
    data class SelectConnection(val connection: ConnectionRef?) : GraphynEditorIntent
    data object DeleteSelectedConnection : GraphynEditorIntent
    data class MoveNode(val nodeId: String, val delta: IntOffset) : GraphynEditorIntent
    data class BeginConnection(val fromNodeId: String, val fromPort: String, val isFromInput: Boolean = false) : GraphynEditorIntent
    data class CompleteConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent
    data class AddNode(val spec: NodeSpec) : GraphynEditorIntent
    data class AddNodeAndConnect(val spec: NodeSpec, val toPort: String, val worldPosition: Offset) : GraphynEditorIntent
    data class UpdateConnectionDraftPosition(val position: Offset?) : GraphynEditorIntent
    data class UpdateViewportTransform(
        val pan: Offset,
        val zoom: Float,
        val focus: Offset,
    ) : GraphynEditorIntent
    data object CancelConnection : GraphynEditorIntent
    data class ReconnectSelectedConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent
    data class ShowNodePicker(val screenPosition: Offset, val worldPosition: Offset) : GraphynEditorIntent
    data object DismissNodePicker : GraphynEditorIntent
}
