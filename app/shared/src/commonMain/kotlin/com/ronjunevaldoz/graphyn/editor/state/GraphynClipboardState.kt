package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.NodeRef

internal class GraphynClipboardState {
    var clipboard: List<Pair<NodeRef, IntOffset>> = emptyList()
        private set

    val hasContent get() = clipboard.isNotEmpty()

    fun copy(nodes: List<Pair<NodeRef, IntOffset>>) {
        clipboard = nodes
    }
}
