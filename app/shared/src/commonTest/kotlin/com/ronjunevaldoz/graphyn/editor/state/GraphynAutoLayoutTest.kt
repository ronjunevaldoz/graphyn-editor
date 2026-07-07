package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import kotlin.test.Test
import kotlin.test.assertTrue

class GraphynAutoLayoutTest {
    private val size = IntSize(280, 180)

    private fun node(id: String) = NodeRef(id, "test")
    private fun conn(from: String, to: String) = ConnectionRef(from, "out", to, "in")

    private fun rectsOverlap(a: IntOffset, b: IntOffset): Boolean =
        a.x < b.x + size.width && b.x < a.x + size.width && a.y < b.y + size.height && b.y < a.y + size.height

    private fun assertNoOverlaps(positions: Map<String, IntOffset>) {
        val entries = positions.entries.toList()
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                assertTrue(
                    !rectsOverlap(entries[i].value, entries[j].value),
                    "${entries[i].key} overlaps ${entries[j].key}: ${entries[i].value} vs ${entries[j].value}",
                )
            }
        }
    }

    @Test
    fun diamondDependencyChildDoesNotOverlapEitherParentBranch() {
        // root -> b -> d, root -> c -> d (d shares both b and c as parents)
        val nodes = listOf("root", "b", "c", "d").map(::node)
        val connections = listOf(conn("root", "b"), conn("root", "c"), conn("b", "d"), conn("c", "d"))

        val positions = GraphynAutoLayout.computePositions(nodes, connections) { size }

        assertNoOverlaps(positions)
        // d should land between b and c, not stacked under whichever parent was visited first.
        val bY = positions.getValue("b").y
        val cY = positions.getValue("c").y
        val dY = positions.getValue("d").y
        assertTrue(dY in minOf(bY, cY)..maxOf(bY, cY) + size.height, "expected d ($dY) between b ($bY) and c ($cY)")
    }

    @Test
    fun wideBranchingTreeKeepsAllSiblingsNonOverlapping() {
        // one root with four children, each with its own child — a wide, uneven tree.
        val roots = listOf("root")
        val children = (1..4).map { "child$it" }
        val grandchildren = (1..4).map { "grandchild$it" }
        val nodes = (roots + children + grandchildren).map(::node)
        val connections = children.map { conn("root", it) } +
            children.zip(grandchildren) { c, g -> conn(c, g) }

        val positions = GraphynAutoLayout.computePositions(nodes, connections) { size }

        assertNoOverlaps(positions)
    }

    @Test
    fun cyclicGraphStillProducesAPositionForEveryNodeWithoutHanging() {
        // a -> b -> c -> a is a cycle; d depends on a to confirm the rest of the graph still lays out.
        val nodes = listOf("a", "b", "c", "d").map(::node)
        val connections = listOf(conn("a", "b"), conn("b", "c"), conn("c", "a"), conn("a", "d"))

        val positions = GraphynAutoLayout.computePositions(nodes, connections) { size }

        assertTrue(positions.keys == setOf("a", "b", "c", "d"))
        assertNoOverlaps(positions)
    }

    @Test
    fun isolatedNodesAreGridPlacedBelowTheConnectedGraph() {
        val nodes = listOf(node("root"), node("child"), node("loner1"), node("loner2"))
        val connections = listOf(conn("root", "child"))

        val positions = GraphynAutoLayout.computePositions(nodes, connections) { size }

        val dagBottom = maxOf(positions.getValue("root").y, positions.getValue("child").y) + size.height
        assertTrue(positions.getValue("loner1").y >= dagBottom)
        assertTrue(positions.getValue("loner2").y >= dagBottom)
        assertNoOverlaps(positions)
    }
}
