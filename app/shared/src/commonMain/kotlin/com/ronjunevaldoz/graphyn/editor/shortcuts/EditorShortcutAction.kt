package com.ronjunevaldoz.graphyn.editor.shortcuts

/**
 * A remappable editor action. Each action has a stable ID, display label, and a default [KeyChord].
 * The shortcut state stores *overrides* only, so defaults are always available and changing code
 * defaults doesn't lose user rebinds (they override the new default).
 */
enum class EditorShortcutAction(
    val id: String,
    val label: String,
    val defaultChord: KeyChord,
) {
    Undo("undo", "Undo", KeyChord("Z", primaryMeta = true)),
    Redo("redo", "Redo", KeyChord("Z", primaryMeta = true, shift = true)),
    Copy("copy", "Copy", KeyChord("C", primaryMeta = true)),
    Paste("paste", "Paste", KeyChord("V", primaryMeta = true)),
    Duplicate("duplicate", "Duplicate", KeyChord("D", primaryMeta = true)),
    SelectAll("selectAll", "Select All", KeyChord("A", primaryMeta = true)),
    AutoLayout("autoLayout", "Auto Layout", KeyChord("L", primaryMeta = true, shift = true)),
    Group("group", "Group Selection", KeyChord("G", primaryMeta = true)),
    CollapseToSubgraph("collapseToSubgraph", "Collapse to Subgraph", KeyChord("G", primaryMeta = true, shift = true)),
    ;

    companion object {
        fun byId(id: String): EditorShortcutAction? = entries.firstOrNull { it.id == id }
    }
}
