package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

internal object GraphynAutoLayout {
    private const val HORIZ_GAP = 120
    private const val VERT_GAP = 80
    internal const val MAX_NODES = 20

    fun computePositions(
        nodes: List<NodeRef>,
        connections: List<ConnectionRef>,
        nodeSize: (nodeType: String) -> IntSize = { GraphynCanvasMetrics.NodeSize },
    ): Map<String, IntOffset> {
        if (nodes.isEmpty() || nodes.size > MAX_NODES) return emptyMap()
        val sizes = nodes.associate { it.id to nodeSize(it.type) }
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
        nodes.filter { it.id !in visited }.forEach { topoOrder.add(it.id) }

        // Column x: each column's x = cumulative (max node width + HORIZ_GAP) of preceding columns
        val maxWidthPerCol = mutableMapOf<Int, Int>()
        nodes.forEach { n ->
            val d = depth[n.id] ?: 0
            maxWidthPerCol[d] = maxOf(maxWidthPerCol.getOrElse(d) { 0 }, sizes[n.id]?.width ?: 0)
        }
        val maxDepth = depth.values.maxOrNull() ?: 0
        val colX = IntArray(maxDepth + 1)
        for (d in 1..maxDepth) colX[d] = colX[d - 1] + (maxWidthPerCol[d - 1] ?: 0) + HORIZ_GAP

        // Band height: leaf = nodeHeight + VERT_GAP; branch = max(own height, sum of children).
        // The max ensures a tall parent never gets a band smaller than itself, preventing
        // negative y-offsets when children are shorter than the parent node.
        val fallbackH = { id: String -> (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) + VERT_GAP }
        val bandH = mutableMapOf<String, Int>()
        topoOrder.reversed().forEach { id ->
            val children = outEdges[id] ?: emptyList()
            bandH[id] = if (children.isEmpty()) fallbackH(id)
                        else maxOf(fallbackH(id), children.sumOf { bandH[it] ?: fallbackH(it) })
        }

        // Top-down band assignment; node y = bandStart + (bandH - nodeH) / 2 (top-left)
        val bandStart = mutableMapOf<String, Int>()
        var cursor = 0
        val positions = mutableMapOf<String, IntOffset>()
        topoOrder.forEach { id ->
            if (id !in bandStart) { bandStart[id] = cursor; cursor += bandH[id] ?: 1 }
            val start = bandStart.getValue(id)
            val nodeH = sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height
            val x = colX.getOrElse(depth[id] ?: 0) { 0 }
            val y = start + ((bandH[id] ?: nodeH) - nodeH) / 2
            positions[id] = IntOffset(x, y)
            var childCursor = start
            outEdges[id]?.forEach { cId ->
                if (cId !in bandStart) { bandStart[cId] = childCursor; childCursor += bandH[cId] ?: 1 }
            }
        }

        // Shift layout so its bounding-box centre aligns with the logical canvas centre
        val minX = positions.values.minOf { it.x }
        val maxX = positions.entries.maxOf { (id, p) -> p.x + (sizes[id]?.width ?: GraphynCanvasMetrics.NodeSize.width) }
        val minY = positions.values.minOf { it.y }
        val maxY = positions.entries.maxOf { (id, p) -> p.y + (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) }
        val shiftX = GraphynCanvasBounds.DefaultLogicalCanvasWidth / 2 - (minX + maxX) / 2
        val shiftY = GraphynCanvasBounds.DefaultLogicalCanvasHeight / 2 - (minY + maxY) / 2
        return positions.mapValues { (_, p) -> IntOffset(p.x + shiftX, p.y + shiftY) }
    }
}

internal fun GraphynEditorState.performAutoLayout() {
    val wf = workflow ?: return
    if (wf.nodes.size > GraphynAutoLayout.MAX_NODES) {
        log.push("Auto-layout skipped: ${wf.nodes.size} nodes exceeds limit of ${GraphynAutoLayout.MAX_NODES}")
        return
    }
    val registry = canvasCards
    val nodeSize: (String) -> IntSize = { type ->
        registry?.resolve(type)?.let { IntSize(it.nodeWidth, it.nodeHeight) } ?: GraphynCanvasMetrics.NodeSize
    }
    val positions = GraphynAutoLayout.computePositions(wf.nodes, wf.connections, nodeSize)
    positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
}
