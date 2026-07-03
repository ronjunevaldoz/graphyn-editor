package com.ronjunevaldoz.graphyn.editor.canvas

import kotlin.test.Test
import kotlin.test.assertTrue

class GraphynCanvasBoundsTest {
    @Test
    fun defaultLogicalCanvasBoundsAreExpandedForLargeWorkspaces() {
        val bounds = GraphynCanvasBounds()

        assertTrue(bounds.width >= 8192)
        assertTrue(bounds.height >= 6144)
    }
}
