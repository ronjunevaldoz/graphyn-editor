package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.shortcuts.EditorShortcutAction
import com.ronjunevaldoz.graphyn.editor.shortcuts.GraphynShortcutState
import com.ronjunevaldoz.graphyn.editor.shortcuts.KeyChord
import com.ronjunevaldoz.graphyn.editor.shortcuts.RebindResult
import com.ronjunevaldoz.graphyn.editor.shortcuts.ShortcutKeyTable

private val MODIFIER_KEYS = setOf(
    Key.ShiftLeft, Key.ShiftRight, Key.CtrlLeft, Key.CtrlRight,
    Key.MetaLeft, Key.MetaRight, Key.AltLeft, Key.AltRight,
)

@Composable
internal fun ShortcutRow(
    action: EditorShortcutAction,
    shortcutState: GraphynShortcutState,
    recording: Boolean,
    onStartRecording: () -> Unit,
    onResult: (String?) -> Unit,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val chord = shortcutState.chordFor(action)
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(action.label, style = type.bodySmall.copy(color = colors.textPrimary))

        val chipInteraction = remember { MutableInteractionSource() }
        val chipBg = if (recording) colors.accent.copy(alpha = 0.2f) else colors.panelBackground
        val chipBorder = if (recording) colors.accent else colors.border
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(chipBg)
                .border(1.dp, chipBorder, RoundedCornerShape(5.dp))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event -> if (recording) handleRecord(event, action, shortcutState, onResult) else false }
                .clickable(interactionSource = chipInteraction, indication = null) { onStartRecording() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            BasicText(
                if (recording) "Press keys…" else chord.display(),
                style = type.mono.copy(color = if (recording) colors.accent else colors.textSecondary),
            )
        }
        if (recording) LaunchedEffect(action.id) { focusRequester.requestFocus() }
    }
}

private fun handleRecord(
    event: androidx.compose.ui.input.key.KeyEvent,
    action: EditorShortcutAction,
    shortcutState: GraphynShortcutState,
    onResult: (String?) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key in MODIFIER_KEYS) return true
    val keyName = ShortcutKeyTable.keyToName(event.key)
        ?: run { onResult("Unsupported key"); return true }
    val chord = KeyChord(
        keyName = keyName,
        primaryMeta = event.isMetaPressed || event.isCtrlPressed,
        shift = event.isShiftPressed,
    )
    val result = when (val r = shortcutState.rebind(action, chord)) {
        RebindResult.Success -> null
        RebindResult.UnsupportedKey -> "Unsupported key"
        is RebindResult.ConflictWith -> "Already used by ${r.action.label}"
    }
    onResult(result)
    return true
}
