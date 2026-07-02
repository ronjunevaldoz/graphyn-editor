package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Offline [WorkflowGenerator] that ignores the model and builds a small linear graph from the
 * first few catalog specs, wiring each node's first output to the next node's first input where
 * the ports line up. Used for UI development and when no Ollama host is configured.
 *
 * Deterministic enough to demo, with a short [delayMs] to exercise loading states.
 */
class PlaceholderWorkflowGenerator(
    private val delayMs: Long = 400,
    private val maxNodes: Int = 3,
) : WorkflowGenerator {

    override suspend fun generate(prompt: String, catalog: List<NodeSpec>): WorkflowGenerationResult {
        if (delayMs > 0) delay(delayMs)
        if (catalog.isEmpty()) {
            return WorkflowGenerationResult.Failure("No node types available to generate from.")
        }
        val picked = catalog.take(maxNodes)
        val nodes = picked.mapIndexed { i, spec -> NodeRef(id = "${spec.type}-$i", type = spec.type) }
        val nodePositions = nodes.mapIndexed { i, node -> node.id to WorkflowNodePosition(x = (i % 2) * 360, y = (i / 2) * 220) }.toMap()
        val connections = buildList {
            for (i in 0 until nodes.size - 1) {
                val out = picked[i].outputs.firstOrNull()?.name ?: continue
                val into = picked[i + 1].inputs.firstOrNull()?.name ?: continue
                add(ConnectionRef(nodes[i].id, out, nodes[i + 1].id, into))
            }
        }
        val workflow = WorkflowDefinition(
            id = "wf-${Random.nextLong().and(0xFFFFFFFFL)}",
            name = prompt.take(40).ifBlank { "Generated Workflow" },
            nodes = nodes,
            connections = connections,
            nodePositions = nodePositions,
        )
        return WorkflowGenerationResult.Success(workflow)
    }
}
