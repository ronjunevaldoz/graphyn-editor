package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowNodePosition
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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
        val nodePositions: Map<String, WorkflowNodePosition> = emptyMap(),
    )

    @Serializable
    private data class RawNode(
        val id: String,
        val type: String,
        val config: Map<String, JsonElement> = emptyMap(),
    )

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
            nodes = validNodes.map { node ->
                NodeRef(id = node.id, type = node.type, config = coerceConfig(node, specByType.getValue(node.type)))
            },
            connections = validConnections.map {
                ConnectionRef(it.fromNodeId, it.fromPort, it.toNodeId, it.toPort)
            },
            nodePositions = parsed.nodePositions,
        )
        return WorkflowGenerationResult.Success(workflow, droppedNodes, droppedConnections)
    }

    /** Maps a raw node's `config` entries onto known input ports, coercing each JSON value to the port's type. */
    private fun coerceConfig(node: RawNode, spec: NodeSpec): Map<String, WorkflowValue> {
        if (node.config.isEmpty()) return emptyMap()
        val inputsByName = spec.inputs.associateBy { it.name }
        return buildMap {
            node.config.forEach { (port, element) ->
                val portSpec = inputsByName[port] ?: return@forEach
                coerceValue(element, portSpec)?.let { put(port, it) }
            }
        }
    }

    private fun coerceValue(element: JsonElement, port: PortSpec): WorkflowValue? {
        val primitive = element as? JsonPrimitive ?: return null
        return coerceByType(primitive, port.type)
    }

    private fun coerceByType(p: JsonPrimitive, type: WorkflowType): WorkflowValue? = when (type) {
        is WorkflowType.IntType -> p.intOrNull?.let { WorkflowValue.IntValue(it) }
            ?: p.content.toIntOrNull()?.let { WorkflowValue.IntValue(it) }
        is WorkflowType.DoubleType -> p.doubleOrNull?.let { WorkflowValue.DoubleValue(it) }
            ?: p.content.toDoubleOrNull()?.let { WorkflowValue.DoubleValue(it) }
        is WorkflowType.BooleanType -> p.booleanOrNull?.let { WorkflowValue.BooleanValue(it) }
            ?: p.content.toBooleanStrictOrNull()?.let { WorkflowValue.BooleanValue(it) }
        is WorkflowType.NullableType -> if (p.content == "null") WorkflowValue.NullValue else coerceByType(p, type.wrappedType)
        is WorkflowType.StringType, is WorkflowType.EnumType -> WorkflowValue.StringValue(p.content)
        else -> WorkflowValue.StringValue(p.content)
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
