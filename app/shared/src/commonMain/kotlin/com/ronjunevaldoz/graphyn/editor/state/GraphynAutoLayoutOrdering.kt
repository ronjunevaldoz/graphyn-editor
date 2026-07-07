package com.ronjunevaldoz.graphyn.editor.state

internal object GraphynAutoLayoutOrdering {
    class Ordering(val depth: Map<String, Int>, val columns: List<List<String>>)

    fun compute(
        ids: List<String>,
        inEdges: Map<String, List<String>>,
        outEdges: Map<String, List<String>>,
        minimizeCrossings: Boolean,
    ): Ordering {
        // Kahn BFS: topological order + longest-path depth (= column index). If a cycle blocks
        // further progress, force-enqueue the unvisited node with the fewest unresolved in-edges
        // so cyclic graphs still get a stable column/order instead of being dumped unordered.
        val depth = ids.associateWith { 0 }.toMutableMap()
        val topoOrder = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        ids.filter { inEdges[it].isNullOrEmpty() }.forEach { queue.add(it) }

        while (visited.size < ids.size) {
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                topoOrder.add(id)
                outEdges[id]?.forEach { succ ->
                    depth[succ] = maxOf(depth[succ] ?: 0, (depth[id] ?: 0) + 1)
                    if (succ !in visited && inEdges[succ]?.all { it in visited } == true) queue.add(succ)
                }
            }
            val remaining = ids.filter { it !in visited }
            if (remaining.isEmpty()) break
            queue.add(remaining.minBy { id -> inEdges[id]?.count { it !in visited } ?: 0 })
        }

        val maxDepth = depth.values.maxOrNull() ?: 0
        val columns = List(maxDepth + 1) { mutableListOf<String>() }
        topoOrder.forEach { columns[depth[it] ?: 0].add(it) }
        if (minimizeCrossings) barycenterSweep(columns, inEdges, outEdges)
        return Ordering(depth, columns)
    }

    // Barycenter sweeps: sort each column by the average row index of its neighbours in the
    // adjacent column (down pass uses parents, up pass uses children). Two rounds is enough
    // for graphs under the MAX_NODES cap; further rounds change almost nothing.
    private fun barycenterSweep(
        columns: List<MutableList<String>>,
        inEdges: Map<String, List<String>>,
        outEdges: Map<String, List<String>>,
    ) {
        repeat(2) {
            for (d in 1 until columns.size) sortByNeighbours(columns[d], columns[d - 1], inEdges)
            for (d in columns.size - 2 downTo 0) sortByNeighbours(columns[d], columns[d + 1], outEdges)
        }
    }

    private fun sortByNeighbours(
        column: MutableList<String>,
        adjacent: List<String>,
        edges: Map<String, List<String>>,
    ) {
        val adjacentRow = adjacent.withIndex().associate { (i, id) -> id to i }
        val currentRow = column.withIndex().associate { (i, id) -> id to i }
        val sorted = column.sortedBy { id ->
            val neighbours = edges[id].orEmpty().mapNotNull { adjacentRow[it] }
            if (neighbours.isEmpty()) currentRow.getValue(id).toDouble() else neighbours.average()
        }
        column.clear()
        column.addAll(sorted)
    }
}
