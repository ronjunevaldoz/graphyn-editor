package com.ronjunevaldoz.graphyn.editor.shortcuts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShortcutStateTest {

    @Test
    fun defaultChordForAction() {
        val state = GraphynShortcutState()
        assertEquals(EditorShortcutAction.Undo.defaultChord, state.chordFor(EditorShortcutAction.Undo))
        assertEquals(EditorShortcutAction.Group.defaultChord, state.chordFor(EditorShortcutAction.Group))
    }

    @Test
    fun rebindSucceeds() {
        val state = GraphynShortcutState()
        val newChord = KeyChord("F1", primaryMeta = true, shift = false)
        val result = state.rebind(EditorShortcutAction.Copy, newChord)
        assertEquals(RebindResult.Success, result)
        assertEquals(newChord, state.chordFor(EditorShortcutAction.Copy))
    }

    @Test
    fun rebindDetectsConflict() {
        val state = GraphynShortcutState()
        val undoChord = state.chordFor(EditorShortcutAction.Undo)
        val result = state.rebind(EditorShortcutAction.Copy, undoChord)
        assertTrue(result is RebindResult.ConflictWith)
        assertEquals(EditorShortcutAction.Undo, (result as RebindResult.ConflictWith).action)
    }

    @Test
    fun resetToDefaultReverts() {
        val state = GraphynShortcutState()
        val newChord = KeyChord("X", primaryMeta = true)
        state.rebind(EditorShortcutAction.Copy, newChord)
        state.resetToDefault(EditorShortcutAction.Copy)
        assertEquals(EditorShortcutAction.Copy.defaultChord, state.chordFor(EditorShortcutAction.Copy))
    }

    @Test
    fun resetAllClearsAllOverrides() {
        val state = GraphynShortcutState()
        state.rebind(EditorShortcutAction.Copy, KeyChord("X", primaryMeta = true))
        state.rebind(EditorShortcutAction.Paste, KeyChord("Y", primaryMeta = true))
        state.resetAll()
        assertEquals(EditorShortcutAction.Copy.defaultChord, state.chordFor(EditorShortcutAction.Copy))
        assertEquals(EditorShortcutAction.Paste.defaultChord, state.chordFor(EditorShortcutAction.Paste))
    }

    @Test
    fun keychordDisplay() {
        val chord1 = KeyChord("Z", primaryMeta = true)
        assertEquals("Ctrl+Z", chord1.display(isMac = false))
        assertEquals("cmd+Z", chord1.display(isMac = true))

        val chord2 = KeyChord("G", primaryMeta = true, shift = true)
        assertEquals("Ctrl+Shift+G", chord2.display(isMac = false))
        assertEquals("cmd+shift+G", chord2.display(isMac = true))
    }

    @Test
    fun keychordConflictDetection() {
        val c1 = KeyChord("Z", primaryMeta = true)
        val c2 = KeyChord("Z", primaryMeta = true)
        val c3 = KeyChord("Z", shift = true)
        assertTrue(c1.conflictsWith(c2))
        assertTrue(!c1.conflictsWith(c3))
    }
}
