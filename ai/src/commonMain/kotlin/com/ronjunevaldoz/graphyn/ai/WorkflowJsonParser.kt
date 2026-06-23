package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses raw LLM output into a validated [WorkflowDefinition]. Pure and testable.
 *
 * LLMs are unreliable: they wrap JSON in markdown fences, emit unknown node types, and produce
 * dangling connections. This parser is defensive — it extracts the JSON object, drops nodes whose
 * type isn't in the [catalog], and drops connections whose endpoints/ports don't resolve, reporting
 * what it sanitized rather than failing outright.
 */
internal object WorkflowJsonParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class RawWorkflow(
        val id: String? = null,
        val name: String? = null,
        val nodes: List<RawNode> = emptyList(),
        val connections: List<RawConnection> = emptyList(),
    )

    @Serializable
    private data class RawNode(val id: String, val type: String)

    @Serializable
    private data class RawConnection(
        val fromNodeId: String,
        val fromPort: String,
        val toNodeId: String,
        val toPort: String,
    )

    fun parse(raw: String, catalog: List<NodeSpec>, fallbackId: String): WorkflowGenerationResult {
        val jsonText = extractJsonObject(raw)
            ?: return WorkflowGenerationResult.Failure("Model did not return a JSON object.")
        val parsed = try {
            json.decodeFromString<RawWorkflow>(jsonText)
        } catch (e: Exception) {
            return WorkflowGenerationResult.Failure("Could not parse model output as a workflow.")
        }

        val specByType = catalog.associateBy { it.type }
        val droppedNodes = mutableListOf<String>()
        val validNodes = parsed.nodes.filter { node ->
            (node.type in specByType).also { if (!it) droppedNodes.add("${node.id} (${node.type})") }
        }.distinctBy { it.id }
        if (validNodes.isEmpty()) {
            return WorkflowGenerationResult.Failure("Model produced no valid nodes for this catalog.")
        }

        val nodeTypeById = validNodes.associate { it.id to it.type }
        var droppedConnections = 0
        val validConnections = parsed.connections.filter { c ->
            val ok = isValidConnection(c, nodeTypeById, specByType)
            if (!ok) droppedConnections++
            ok
        }.distinct()

        val workflow = WorkflowDefinition(
            id = parsed.id?.takeIf { it.isNotBlank() } ?: fallbackId,
            name = parsed.name?.takeIf { it.isNotBlank() } ?: "Generated Workflow",
            nodes = validNodes.map { NodeRef(id = it.id, type = it.type) },
            connections = validConnections.map {
                ConnectionRef(it.fromNodeId, it.fromPort, it.toNodeId, it.toPort)
            },
        )
        return WorkflowGenerationResult.Success(workflow, droppedNodes, droppedConnections)
    }

    private fun isValidConnection(
        c: RawConnection,
        nodeTypeById: Map<String, String>,
        specByType: Map<String, NodeSpec>,
    ): Boolean {
        val fromSpec = specByType[nodeTypeById[c.fromNodeId]] ?: return false
        val toSpec = specByType[nodeTypeById[c.toNodeId]] ?: return false
        return fromSpec.outputs.any { it.name == c.fromPort } && toSpec.inputs.any { it.name == c.toPort }
    }

    /** Pulls the first balanced top-level `{...}` out of a possibly fenced/prose-wrapped string. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until raw.length) {
            when (raw[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
