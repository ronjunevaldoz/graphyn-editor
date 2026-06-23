package com.ronjunevaldoz.graphyn.editor.shortcuts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.ronjunevaldoz.graphyn.editor.theme.GraphynSettingsStore
import com.ronjunevaldoz.graphyn.editor.theme.rememberGraphynSettingsStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SHORTCUTS_OVERRIDES_KEY = "graphyn.shortcuts.overrides"

class GraphynShortcutState(
    private val settingsStore: GraphynSettingsStore? = null,
) {
    private var _overrides by mutableStateOf<Map<String, KeyChord>>(emptyMap())

    init {
        _overrides = loadOverridesFromStore()
    }

    fun chordFor(action: EditorShortcutAction): KeyChord =
        _overrides[action.id] ?: action.defaultChord

    fun resolveAction(event: KeyEvent): EditorShortcutAction? {
        val eventKeyName = ShortcutKeyTable.keyToName(event.key) ?: return null
        val eventChord = KeyChord(
            keyName = eventKeyName,
            primaryMeta = event.isMetaPressed || event.isCtrlPressed,
            shift = event.isShiftPressed,
        )
        return EditorShortcutAction.entries.firstOrNull { chordFor(it).matches(event) }
    }

    fun rebind(action: EditorShortcutAction, newChord: KeyChord): RebindResult {
        if (ShortcutKeyTable.keyFromName(newChord.keyName) == null) {
            return RebindResult.UnsupportedKey
        }
        val conflict = EditorShortcutAction.entries
            .filter { it != action }
            .firstOrNull { newChord.conflictsWith(chordFor(it)) }
        if (conflict != null) {
            return RebindResult.ConflictWith(conflict)
        }
        _overrides = _overrides + (action.id to newChord)
        persistOverridesToStore()
        return RebindResult.Success
    }

    fun resetToDefault(action: EditorShortcutAction) {
        _overrides = _overrides - action.id
        persistOverridesToStore()
    }

    fun resetAll() {
        _overrides = emptyMap()
        persistOverridesToStore()
    }

    private fun loadOverridesFromStore(): Map<String, KeyChord> {
        val json = settingsStore?.getString(SHORTCUTS_OVERRIDES_KEY) ?: return emptyMap()
        return try {
            Json.decodeFromString<Map<String, KeyChord>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun persistOverridesToStore() {
        if (settingsStore == null) return
        if (_overrides.isEmpty()) {
            settingsStore.putString(SHORTCUTS_OVERRIDES_KEY, null)
        } else {
            settingsStore.putString(SHORTCUTS_OVERRIDES_KEY, Json.encodeToString(_overrides))
        }
    }
}

sealed class RebindResult {
    data object Success : RebindResult()
    data class ConflictWith(val action: EditorShortcutAction) : RebindResult()
    data object UnsupportedKey : RebindResult()
}

@Composable
fun rememberGraphynShortcutState(): GraphynShortcutState {
    val settingsStore = rememberGraphynSettingsStore()
    return remember(settingsStore) {
        GraphynShortcutState(settingsStore)
    }
}
