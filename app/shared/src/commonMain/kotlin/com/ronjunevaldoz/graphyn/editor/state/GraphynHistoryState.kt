package com.ronjunevaldoz.graphyn.editor.state

internal class GraphynHistoryState(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<GraphynEditorSnapshot>()
    private val redoStack = ArrayDeque<GraphynEditorSnapshot>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    fun push(snapshot: GraphynEditorSnapshot) {
        undoStack.addLast(snapshot)
        if (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: GraphynEditorSnapshot): GraphynEditorSnapshot? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: GraphynEditorSnapshot): GraphynEditorSnapshot? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }
}
