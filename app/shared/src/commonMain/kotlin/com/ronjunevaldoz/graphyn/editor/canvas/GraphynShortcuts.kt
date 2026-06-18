package com.ronjunevaldoz.graphyn.editor.canvas

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

// Cmd on macOS, Ctrl on Windows/Linux
internal val KeyEvent.isPrimaryMeta: Boolean
    get() = isMetaPressed || isCtrlPressed

internal object GraphynShortcuts {
    fun isUndo(e: KeyEvent) = e.isPrimaryMeta && !e.isShiftPressed && e.key == Key.Z
    fun isRedo(e: KeyEvent) = e.isPrimaryMeta && e.isShiftPressed && e.key == Key.Z
    fun isCopy(e: KeyEvent) = e.isPrimaryMeta && !e.isShiftPressed && e.key == Key.C
    fun isPaste(e: KeyEvent) = e.isPrimaryMeta && !e.isShiftPressed && e.key == Key.V
    fun isDuplicate(e: KeyEvent) = e.isPrimaryMeta && !e.isShiftPressed && e.key == Key.D
    fun isSelectAll(e: KeyEvent) = e.isPrimaryMeta && !e.isShiftPressed && e.key == Key.A
}
