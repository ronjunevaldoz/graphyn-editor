package com.ronjunevaldoz.graphyn.editor.interaction

import androidx.compose.ui.geometry.Offset

data class GraphynNodePickerState(
    val screenPosition: Offset,
    val worldPosition: Offset,
    val draft: GraphynConnectionDraft,
)
