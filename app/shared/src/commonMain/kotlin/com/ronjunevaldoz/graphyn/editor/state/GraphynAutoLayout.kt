package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasBounds
import com.ronjunevaldoz.graphyn.editor.canvas.GraphynCanvasMetrics
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory

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

        // Kahn BFS: topological order + longest-path depth (= column index)
        val depth = connected.associate { it.id to 0 }.toMutableMap()
        val topoOrder = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        connected.filter { inEdges[it.id].isNullOrEmpty() }.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            topoOrder.add(id)
            outEdges[id]?.forEach { succ ->
                depth[succ] = maxOf(depth[succ] ?: 0, (depth[id] ?: 0) + 1)
                if (inEdges[succ]?.all { it in visited } == true) queue.add(succ)
            }
        }
        connected.filter { it.id !in visited }.forEach { topoOrder.add(it.id) }

        // Column x: each column's x = cumulative (max node width + horizGap) of preceding columns
        val maxWidthPerCol = mutableMapOf<Int, Int>()
        connected.forEach { n ->
            val d = depth[n.id] ?: 0
            maxWidthPerCol[d] = maxOf(maxWidthPerCol.getOrElse(d) { 0 }, sizes[n.id]?.width ?: 0)
        }
        val maxDepth = depth.values.maxOrNull() ?: 0
        val colX = IntArray(maxDepth + 1)
        for (d in 1..maxDepth) colX[d] = colX[d - 1] + (maxWidthPerCol[d - 1] ?: 0) + horizGap

        // Band height: leaf = nodeHeight + VERT_GAP; branch = max(own height, sum of children).
        // The max ensures a tall parent never gets a band smaller than itself, preventing
        // negative y-offsets when children are shorter than the parent node.
        val fallbackH = { id: String -> (sizes[id]?.height ?: GraphynCanvasMetrics.NodeSize.height) + vertGap }
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

internal data class AutoLayoutResult(
    val positions: Map<String, IntOffset>,
    val sizes: Map<String, IntSize>,
)

internal fun GraphynEditorState.performAutoLayout(): AutoLayoutResult? {
    val wf = workflow ?: return null
    val registry = canvasCards
    val nodeSize: (String) -> IntSize = { type ->
        registry?.resolve(type)?.let { IntSize(it.nodeWidth, it.nodeHeight) }
            ?: nodeSpecs?.resolve(type)?.let { spec ->
                val f = FieldCardFactory(inputRows = spec.inputs.size, outputRows = spec.outputs.size)
                IntSize(f.nodeWidth, f.nodeHeight)
            }
            ?: GraphynCanvasMetrics.NodeSize
    }
    // Annotations (sticky notes, frames) are not part of the dataflow DAG; lay out the graph
    // nodes, then park annotations in a column to the left so they read as a legend.
    val (annotationNodes, graphNodes) = wf.nodes.partition { registry?.resolve(it.type)?.isAnnotation == true }
    if (graphNodes.size > GraphynAutoLayout.MAX_NODES) {
        log.push("Auto-layout skipped: ${graphNodes.size} nodes exceeds limit of ${GraphynAutoLayout.MAX_NODES}")
        return null
    }
    val positions = GraphynAutoLayout.computePositions(graphNodes, wf.connections, nodeSize).toMutableMap()
    if (annotationNodes.isNotEmpty()) {
        val gap = GraphynCanvasMetrics.NodeSize.width
        val maxAnnW = annotationNodes.maxOf { nodeSize(it.type).width }
        val graphMinX = positions.values.minOfOrNull { it.x } ?: 0
        val graphMinY = positions.values.minOfOrNull { it.y } ?: 0
        val annX = graphMinX - maxAnnW - gap
        var annY = graphMinY
        annotationNodes.forEach { ann ->
            positions[ann.id] = IntOffset(annX, annY)
            annY += nodeSize(ann.type).height + GraphynCanvasMetrics.NodeSize.height
        }
    }
    positions.forEach { (id, pos) -> layout.setNodePosition(id, pos) }
    val sizes = wf.nodes.associate { it.id to nodeSize(it.type) }
    return AutoLayoutResult(positions, sizes)
}
