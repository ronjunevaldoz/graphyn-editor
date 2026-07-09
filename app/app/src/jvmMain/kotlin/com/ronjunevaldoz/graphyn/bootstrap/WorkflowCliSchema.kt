package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private val schemaJson = Json { ignoreUnknownKeys = true }

internal fun readStoryboardField(storyboardJsonPath: String, field: String): String =
    schemaJson.parseToJsonElement(File(storyboardJsonPath).readText()).jsonObject[field]?.jsonPrimitive?.content.orEmpty()

internal fun readStoryboardScenePrompt(storyboardJsonPath: String, index: Int): String {
    val scenes = schemaJson.parseToJsonElement(File(storyboardJsonPath).readText()).jsonObject["scenes"]?.jsonArray
    return scenes?.getOrNull(index)?.jsonObject?.get("prompt")?.jsonPrimitive?.content.orEmpty()
}

internal fun printSchema(workflow: WorkflowDefinition, nodeSpecs: NodeSpecRegistry) {
    println("Schema for '${workflow.name}' — overridable inputs (node-path: type, description):")
    computeWorkflowSchema(workflow, nodeSpecs).forEach { entry ->
        val optional = if (entry.required) "" else " (optional)"
        val hint = entry.description?.let { " — $it" }.orEmpty()
        println("  ${entry.path}: ${entry.type}$optional$hint")
    }
}

private data class WorkflowSchemaEntry(val path: String, val type: String, val required: Boolean, val description: String?)

private fun computeWorkflowSchema(workflow: WorkflowDefinition, nodeSpecs: NodeSpecRegistry, pathPrefix: String = ""): List<WorkflowSchemaEntry> {
    val connectedTargets = workflow.connections.map { it.toNodeId to it.toPort }.toSet()
    return workflow.nodes.flatMap { node ->
        val ownPorts = nodeSpecs.resolve(node.type)?.inputs.orEmpty().filter { port -> (node.id to port.name) !in connectedTargets }
            .map { port -> WorkflowSchemaEntry("$pathPrefix${node.id}.${port.name}", port.type.toString(), port.required, port.description) }
        val nestedPorts = node.subgraph?.let { computeWorkflowSchema(it, nodeSpecs, "$pathPrefix${node.id}/") }.orEmpty()
        ownPorts + nestedPorts
    }
}

