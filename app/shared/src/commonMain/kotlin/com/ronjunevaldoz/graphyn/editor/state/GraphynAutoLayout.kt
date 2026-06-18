package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef

internal object GraphynAutoLayout {
    private const val COL_GAP = 300
    private const val ROW_GAP = 200

    fun computePositions(
        nodes: List<NodeRef>,
        connections: List<ConnectionRef>,
    ): Map<String, IntOffset> {
        if (nodes.isEmpty()) return emptyMap()
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

        val byDepth = nodes.groupBy { depth[it.id] ?: 0 }
        return buildMap {
            byDepth.entries.sortedBy { it.key }.forEach { (col, nodesAtDepth) ->
                nodesAtDepth.forEachIndexed { row, node ->
                    put(node.id, IntOffset(col * COL_GAP, row * ROW_GAP))
                }
            }
        }
    }
}
