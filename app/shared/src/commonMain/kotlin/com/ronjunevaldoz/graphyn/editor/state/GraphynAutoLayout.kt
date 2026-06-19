package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef

internal object GraphynAutoLayout {
    private const val COL_GAP = 300
    private const val ROW_GAP = 200

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

        val depth = nodes.associate { it.id to 0 }.toMutableMap()
        val queue = ArrayDeque<String>()
        val visited = mutableSetOf<String>()
        nodes.filter { inEdges[it.id].isNullOrEmpty() }.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            outEdges[id]?.forEach { succ ->
                depth[succ] = maxOf(depth[succ] ?: 0, (depth[id] ?: 0) + 1)
                if (inEdges[succ]?.all { it in visited } == true) queue.add(succ)
            }
        }
        nodes.filter { it.id !in visited }.forEach { queue.add(it.id) }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            visited.add(id)
        }

        // Group by depth and assign y positions centered on parent midpoints
        val positions = mutableMapOf<String, IntOffset>()
        nodes.groupBy { depth[it.id] ?: 0 }.entries.sortedBy { it.key }.forEach { (col, nodesAtDepth) ->
            // Sort within column by average parent y so siblings stay near their parent
            val sorted = nodesAtDepth.sortedBy { node ->
                val ys = inEdges[node.id]?.mapNotNull { positions[it]?.y?.toDouble() } ?: emptyList()
                if (ys.isEmpty()) Double.NEGATIVE_INFINITY else ys.average()
            }
            // Place each node at its parents' midpoint; push down to enforce ROW_GAP
            var cursor = 0
            sorted.forEach { node ->
                val ys = inEdges[node.id]?.mapNotNull { positions[it]?.y } ?: emptyList()
                val preferred = if (ys.isEmpty()) cursor else ys.average().toInt()
                val y = maxOf(cursor, preferred)
                positions[node.id] = IntOffset(col * COL_GAP, y)
                cursor = y + ROW_GAP
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
