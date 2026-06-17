package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class GraphynDebugLogState {
    private companion object {
        const val MaxEntries = 12
    }

    var entries by mutableStateOf<List<String>>(emptyList())

    fun push(message: String) {
        entries = (entries + message).takeLast(MaxEntries)
    }
}
