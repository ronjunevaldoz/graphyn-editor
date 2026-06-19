package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef

internal object GraphynAutoLayout {
    private const val COL_GAP = 320
    private const val ROW_GAP = 220

    internal const val MAX_NODES = 20

    fun computePositions(
        nodes: List<NodeRef>,
        connections: List<ConnectionRef>,
    ): Map<String, IntOffset> {
        if (nodes.isEmpty() || nodes.size > MAX_NODES) return emptyMap()
        val nodeIds = nodes.mapTo(mutableSetOf()) { it.id }

        val inEdges = nodes.associate { it.id to mutableListOf<String>() }
        val outEdges = nodes.associate { it.id to mutableListOf<String>() }
        connections.forEach { conn ->
            if (conn.fromNodeId in nodeIds && conn.toNodeId in nodeIds) {
                inEdges[conn.toNodeId]?.add(conn.fromNodeId)
                outEdges[conn.fromNodeId]?.add(conn.toNodeId)
            }
        }

        // Kahn BFS: topological order + longest-path depth (= column index)
        val depth = nodes.associate { it.id to 0 }.toMutableMap()
        val topoOrder = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        nodes.filter { inEdges[it.id].isNullOrEmpty() }.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            topoOrder.add(id)
            outEdges[id]?.forEach { succ ->
                depth[succ] = maxOf(depth[succ] ?: 0, (depth[id] ?: 0) + 1)
                if (inEdges[succ]?.all { it in visited } == true) queue.add(succ)
            }
        }
        // Append disconnected / cyclic nodes so they still receive positions
        nodes.filter { it.id !in visited }.forEach { topoOrder.add(it.id) }

        // Bottom-up: count the number of leaves under each node.
        // Leaves = 1; internal nodes = sum of children's leaf counts.
        // This determines how much vertical band each node should own.
        val leafCount = mutableMapOf<String, Int>()
        topoOrder.reversed().forEach { id ->
            val children = outEdges[id] ?: emptyList()
            leafCount[id] = if (children.isEmpty()) 1 else children.sumOf { leafCount[it] ?: 1 }
        }

        // Top-down band assignment:
        // Each node owns ROW_GAP * leafCount vertical space.
        // The node itself sits at the centre of that band.
        // Children's bands tile inside the parent's band in order.
        val bandStart = mutableMapOf<String, Int>()
        var cursor = 0
        val positions = mutableMapOf<String, IntOffset>()

        topoOrder.forEach { id ->
            if (id !in bandStart) {
                // root or disconnected: claim the next available band
                bandStart[id] = cursor
                cursor += (leafCount[id] ?: 1) * ROW_GAP
            }
            val start = bandStart.getValue(id)
            val size = (leafCount[id] ?: 1) * ROW_GAP
            positions[id] = IntOffset((depth[id] ?: 0) * COL_GAP, start + size / 2)

            // Tile children's sub-bands left-to-right within this band
            var childCursor = start
            outEdges[id]?.forEach { childId ->
                if (childId !in bandStart) {
                    bandStart[childId] = childCursor
                    childCursor += (leafCount[childId] ?: 1) * ROW_GAP
                }
            }
        }

        return positions
    }
}

internal fun GraphynEditorState.performAutoLayout() {
    val wf = workflow ?: return
    if (wf.nodes.size > GraphynAutoLayout.MAX_NODES) {
        log.push("Auto-layout skipped: ${wf.nodes.size} nodes exceeds limit of ${GraphynAutoLayout.MAX_NODES}")
        return
    }
    val positions = GraphynAutoLayout.computePositions(wf.nodes, wf.connections)
    positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
    viewportState.fitToPositions(positions)
}
