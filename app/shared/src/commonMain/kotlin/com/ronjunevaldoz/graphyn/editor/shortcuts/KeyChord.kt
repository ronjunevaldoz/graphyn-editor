package com.ronjunevaldoz.graphyn.editor.shortcuts

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import kotlinx.serialization.Serializable

@Serializable
data class KeyChord(
    val keyName: String,
    val primaryMeta: Boolean = false,
    val shift: Boolean = false,
) {
    fun matches(event: KeyEvent): Boolean {
        val eventKeyName = ShortcutKeyTable.keyToName(event.key) ?: return false
        return eventKeyName == keyName &&
            (event.isMetaPressed || event.isCtrlPressed) == primaryMeta &&
            event.isShiftPressed == shift
    }

    fun display(isMac: Boolean = false): String {
        val parts = mutableListOf<String>()
        if (primaryMeta) parts.add(if (isMac) "cmd" else "Ctrl")
        if (shift) parts.add(if (isMac) "shift" else "Shift")
        parts.add(keyName)
        return parts.joinToString("+")
    }

    fun conflictsWith(other: KeyChord): Boolean =
        keyName == other.keyName &&
        primaryMeta == other.primaryMeta &&
        shift == other.shift
}
