package com.ronjunevaldoz.graphyn.editor.interaction

import androidx.compose.ui.unit.IntOffset

sealed interface GraphynEditorIntent {
    data class SelectNode(val nodeId: String?) : GraphynEditorIntent
    data class MoveNode(val nodeId: String, val delta: IntOffset) : GraphynEditorIntent
    data class BeginConnection(val fromNodeId: String, val fromPort: String) : GraphynEditorIntent
    data class CompleteConnection(val toNodeId: String, val toPort: String) : GraphynEditorIntent
    data object CancelConnection : GraphynEditorIntent
}
