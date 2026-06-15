package com.ronjunevaldoz.graphyn.editor.interaction

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeSpec

sealed interface GraphynEditorIntent {
    data class SelectNode(val nodeId: String?) : GraphynEditorIntent
    data class MoveNode(val nodeId: String, val delta: IntOffset) : GraphynEditorIntent
    data class BeginConnection(val fromNodeId: String, val fromPort: String) : GraphynEditorIntent
    data class CompleteConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent
    data class AddNode(val spec: NodeSpec) : GraphynEditorIntent
    data class UpdateConnectionDraftPosition(val position: Offset?) : GraphynEditorIntent
    data object CancelConnection : GraphynEditorIntent
}
