package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasInset
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics

internal object GraphynAutoLayout {
    private const val GRID_COLS = 3
    internal const val MAX_NODES = 50

    fun computePositions(
        nodes: List<NodeRef>,
        connections: List<ConnectionRef>,
        nodeSize: (nodeType: String) -> IntSize = { GraphynCanvasMetrics.NodeSize },
    ): Map<String, IntOffset> {
        if (nodes.isEmpty() || nodes.size > MAX_NODES) return emptyMap()
        val sizes = nodes.associate { it.id to nodeSize(it.type) }
        val maxW = sizes.values.maxOf { it.width }.coerceAtLeast(GraphynCanvasMetrics.NodeSize.width)
        val maxH = sizes.values.maxOf { it.height }.coerceAtLeast(GraphynCanvasMetrics.NodeSize.height)
        val horizGap = (maxW * 1.5f).toInt()
        val vertGap = (maxH * 1.5f).toInt()
        val nodeIds = nodes.mapTo(mutableSetOf()) { it.id }

        val inEdges = nodes.associate { it.id to mutableListOf<String>() }
        val outEdges = nodes.associate { it.id to mutableListOf<String>() }
        connections.forEach { conn ->
            if (conn.fromNodeId in nodeIds && conn.toNodeId in nodeIds) {
                inEdges[conn.toNodeId]?.add(conn.fromNodeId)
                outEdges[conn.fromNodeId]?.add(conn.toNodeId)
            }
        }

        // Split isolated nodes (no edges) into a grid; layout only connected nodes via DAG.
        val isolated = nodes.filter { inEdges[it.id].isNullOrEmpty() && outEdges[it.id].isNullOrEmpty() }
        val connected = nodes.filter { it !in isolated }

        // Kahn BFS: topological order + longest-path depth (= column index). If a cycle blocks
        // further progress, force-enqueue the unvisited node with the fewest unresolved in-edges
        // so cyclic graphs still get a stable column/order instead of being dumped unordered.
        val depth = connected.associate { it.id to 0 }.toMutableMap()
        val topoOrder = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        connected.filter { inEdges[it.id].isNullOrEmpty() }.forEach { queue.add(it.id) }

        while (visited.size < connected.size) {
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                topoOrder.add(id)
                outEdges[id]?.forEach { succ ->
                    depth[succ] = maxOf(depth[succ] ?: 0, (depth[id] ?: 0) + 1)
                    if (succ !in visited && inEdges[succ]?.all { it in visited } == true) queue.add(succ)
                }
            }
            val remaining = connected.filter { it.id !in visited }
            if (remaining.isEmpty()) break
            queue.add(remaining.minBy { n -> inEdges[n.id]?.count { it !in visited } ?: 0 }.id)
        }

        // Column x: each column's x = cumulative (max node width + horizGap) of preceding columns
        val maxWidthPerCol = mutableMapOf<Int, Int>()
        connected.forEach { n ->
            val d = depth[n.id] ?: 0
            maxWidthPerCol[d] = maxOf(maxWidthPerCol.getOrElse(d) { 0 }, sizes[n.id]?.width ?: 0)
        }
        val maxDepth = depth.values.maxOrNull() ?: 0
        val colX = IntArray(maxDepth + 1)
        for (d in 1..maxDepth) colX[d] = colX[d - 1] + (maxWidthPerCol[d - 1] ?: 0) + horizGap

        // Vertical placement: each node centers on the average y of its already-placed parents
        // (barycenter), falling back to the next free slot in its column for roots or nodes whose
        // parents aren't placed yet (cycle remnants). A per-column cursor guarantees nodes in the
        // same column never overlap, so a shared child (diamond dependency) no longer has its
        // vertical slot double-booked by every parent that reaches it.
        val nextFreeY = mutableMapOf<Int, Int>()
        val positions = mutableMapOf<String, IntOffset>()
        topoOrder.forEach { id ->
            val d = depth[id] ?: 0
            val nodeH = sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height
            val parentCenters = inEdges[id].orEmpty().mapNotNull { pId ->
                positions[pId]?.let { p -> p.y + (sizes[pId]?.height ?: GraphynCanvasMetrics.NodeSize.height) / 2 }
            }
            val floor = nextFreeY.getOrElse(d) { 0 }
            val y = if (parentCenters.isEmpty()) floor
                    else (parentCenters.sum() / parentCenters.size - nodeH / 2).coerceAtLeast(floor)
            positions[id] = IntOffset(colX.getOrElse(d) { 0 }, y)
            nextFreeY[d] = y + nodeH + vertGap
        }

        // Grid-arrange isolated nodes below the DAG block
        val dagBottom = if (positions.isEmpty()) 0
                        else positions.entries.maxOf { (id, p) -> p.y + (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) }
        val gridTop = if (isolated.isEmpty()) dagBottom else dagBottom + vertGap * 2
        isolated.forEachIndexed { i, node ->
            val col = i % GRID_COLS
            val row = i / GRID_COLS
            val cellW = (sizes[node.id]?.width ?: GraphynCanvasMetrics.NodeSize.width) + horizGap
            val cellH = (sizes[node.id]?.height ?: GraphynCanvasMetrics.NodeSize.height) + vertGap
            positions[node.id] = IntOffset(col * cellW, gridTop + row * cellH)
        }

        // Scale down first if the raw layout is wider/taller than the logical canvas can hold —
        // otherwise the downstream position clamp would collapse all overflowing nodes onto the
        // same edge instead of shrinking the spacing between them.
        val rawMinX = positions.values.minOf { it.x }
        val rawMaxX = positions.entries.maxOf { (id, p) -> p.x + (sizes[id]?.width ?: GraphynCanvasMetrics.NodeSize.width) }
        val rawMinY = positions.values.minOf { it.y }
        val rawMaxY = positions.entries.maxOf { (id, p) -> p.y + (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) }
        val availW = (GraphynCanvasBounds.DefaultLogicalCanvasWidth - GraphynCanvasInset * 2).toFloat()
        val availH = (GraphynCanvasBounds.DefaultLogicalCanvasHeight - GraphynCanvasInset * 2).toFloat()
        val scale = minOf(1f, availW / (rawMaxX - rawMinX).coerceAtLeast(1), availH / (rawMaxY - rawMinY).coerceAtLeast(1))
        val scaled = if (scale < 1f) {
            positions.mapValues { (_, p) ->
                IntOffset(rawMinX + ((p.x - rawMinX) * scale).toInt(), rawMinY + ((p.y - rawMinY) * scale).toInt())
            }
        } else positions

        // Shift layout so its bounding-box centre aligns with the logical canvas centre
        val minX = scaled.values.minOf { it.x }
        val maxX = scaled.entries.maxOf { (id, p) -> p.x + (sizes[id]?.width ?: GraphynCanvasMetrics.NodeSize.width) }
        val minY = scaled.values.minOf { it.y }
        val maxY = scaled.entries.maxOf { (id, p) -> p.y + (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) }
        val shiftX = GraphynCanvasBounds.DefaultLogicalCanvasWidth / 2 - (minX + maxX) / 2
        val shiftY = GraphynCanvasBounds.DefaultLogicalCanvasHeight / 2 - (minY + maxY) / 2
        return scaled.mapValues { (_, p) -> IntOffset(p.x + shiftX + GraphynCanvasInset, p.y + shiftY + GraphynCanvasInset) }
    }
}
