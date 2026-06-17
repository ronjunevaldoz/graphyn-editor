package com.ronjunevaldoz.graphyn.editor.interaction

data class GraphynConnectionDraft(
    val fromNodeId: String,
    val fromPort: String,
    val isFromInput: Boolean = false,
)
