package com.ronjunevaldoz.graphyn.mcp

import com.ronjunevaldoz.graphyn.GraphynRunRegistry
import com.ronjunevaldoz.graphyn.GraphynServerRuntime
import com.ronjunevaldoz.graphyn.applyOverrides
import com.ronjunevaldoz.graphyn.core.execution.ExecutionStreamMessage
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.serialization.DefaultWorkflowJsonCodec
import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val MAX_RESPONSE_CHARS = 20_000

/**
 * workflow_publish/workflow_delete share the same store as the desktop editor
 * (~/.graphyn/workflows) — without a namespace boundary, an agent could silently overwrite or
 * delete a user's real hand-built workflow just by reusing its id. Requiring this prefix makes
 * that impossible by construction rather than relying on a per-call confirmation flag.
 */
private const val MCP_ID_PREFIX = "mcp-"
private const val SCOPE_NOTE =
    "Scoped to ids starting with \"$MCP_ID_PREFIX\" — cannot create, overwrite, or delete " +
        "workflows outside this namespace, to avoid touching the desktop editor's own workflows."

private const val KIND_DISCRIMINATOR_NOTE =
    "WorkflowValue fields use a \"kind\" discriminator, e.g. {\"kind\":\"string\",\"value\":\"...\"}."
private const val TYPE_DISCRIMINATOR_NOTE =
    "WorkflowValue fields here use a \"type\" discriminator (different from workflow_publish's " +
        "\"kind\"), e.g. {\"type\":\"string\",\"value\":\"...\"}."
private const val UNSANDBOXED_NOTE =
    "Runs against the real engine: node types like script.eval (arbitrary Kotlin) and " +
        "io.file_write/io.http_request (filesystem/network) are not sandboxed."

/**
 * Registers the generic workflow tools on [server]: list/get/publish/delete/execute/status/
 * node-types, all operating on arbitrary [com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition]s
 * by id or raw JSON — none of them reference a specific workflow, template, or node type.
 */
fun registerWorkflowTools(
    server: Server,
    store: WorkflowStore,
    runtime: GraphynServerRuntime,
    registry: GraphynRunRegistry,
    json: Json,
) {
    server.addTool(
        name = "workflow_list",
        description = "List all stored workflows (id, name, timestamps, version count).",
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = emptyList()),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
    ) { _ -> listWorkflows(store, json) }

    server.addTool(
        name = "workflow_get",
        description = "Fetch the full definition of a stored workflow by id. $KIND_DISCRIMINATOR_NOTE",
        inputSchema = ToolSchema(
            properties = buildJsonObject { putJsonObject("id") { put("type", "string") } },
            required = listOf("id"),
        ),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
    ) { request -> getWorkflow(store, request.arguments) }

    server.addTool(
        name = "workflow_publish",
        description = "Save (create or update) a workflow from its raw WorkflowDefinition JSON. " +
            "Validates before saving. $SCOPE_NOTE $KIND_DISCRIMINATOR_NOTE $UNSANDBOXED_NOTE",
        inputSchema = ToolSchema(
            properties = buildJsonObject { putJsonObject("workflow") { put("type", "string") } },
            required = listOf("workflow"),
        ),
        toolAnnotations = ToolAnnotations(destructiveHint = true, openWorldHint = true),
    ) { request -> publishWorkflow(store, runtime, json, request.arguments) }

    server.addTool(
        name = "workflow_delete",
        description = "Delete a stored workflow and all its version history by id. $SCOPE_NOTE",
        inputSchema = ToolSchema(
            properties = buildJsonObject { putJsonObject("id") { put("type", "string") } },
            required = listOf("id"),
        ),
        toolAnnotations = ToolAnnotations(destructiveHint = true),
    ) { request -> deleteWorkflow(store, request.arguments) }

    server.addTool(
        name = "workflow_execute",
        description = "Run a stored workflow by id. Optional 'overrides' merges values onto node " +
            "config before running, keyed by node id then port name. $TYPE_DISCRIMINATOR_NOTE " +
            "Optional 'async'=true starts the run in the background and returns {runId} " +
            "immediately instead of blocking — poll workflow_execution_status with it. $UNSANDBOXED_NOTE",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("id") { put("type", "string") }
                putJsonObject("overrides") { put("type", "object") }
                putJsonObject("async") { put("type", "boolean") }
            },
            required = listOf("id"),
        ),
        toolAnnotations = ToolAnnotations(destructiveHint = true, openWorldHint = true),
    ) { request -> executeWorkflow(store, runtime, registry, json, request.arguments) }

    server.addTool(
        name = "workflow_execution_status",
        description = "Snapshot of buffered progress/result frames for a workflow_execute run " +
            "started with async=true.",
        inputSchema = ToolSchema(
            properties = buildJsonObject { putJsonObject("runId") { put("type", "string") } },
            required = listOf("runId"),
        ),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
    ) { request -> executionStatus(registry, json, request.arguments) }

    server.addTool(
        name = "workflow_list_node_types",
        description = "List every registered node type (id, ports, config) available for " +
            "authoring workflow_publish graphs.",
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = emptyList()),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
    ) { _ -> listNodeTypes(runtime, json) }
}

@Serializable
private data class RunStarted(val runId: String)

internal suspend fun listWorkflows(store: WorkflowStore, json: Json): CallToolResult =
    ok(json.encodeToString(store.list()))

internal suspend fun getWorkflow(store: WorkflowStore, arguments: JsonObject?): CallToolResult {
    val id = arguments.stringArg("id") ?: return errorResult("Missing required argument 'id'.")
    val workflow = store.load(id) ?: return errorResult("No workflow found with id '$id'.")
    return ok(DefaultWorkflowJsonCodec.encodeToString(workflow))
}

internal suspend fun publishWorkflow(
    store: WorkflowStore,
    runtime: GraphynServerRuntime,
    json: Json,
    arguments: JsonObject?,
): CallToolResult {
    val raw = arguments.stringArg("workflow")
        ?: return errorResult("Missing required argument 'workflow' (WorkflowDefinition JSON).")
    val workflow = runCatching { DefaultWorkflowJsonCodec.decodeFromString(raw) }
        .getOrElse { return errorResult("Invalid workflow JSON: ${it.message}") }
    if (!workflow.id.startsWith(MCP_ID_PREFIX)) {
        return errorResult("Refusing to publish '${workflow.id}': id must start with '$MCP_ID_PREFIX'.")
    }
    val errors = runtime.validator.validate(workflow)
    if (errors.isNotEmpty()) return errorResult("Validation failed: ${json.encodeToString(errors)}")
    val meta = store.save(workflow)
    return ok(json.encodeToString(meta))
}

internal suspend fun deleteWorkflow(store: WorkflowStore, arguments: JsonObject?): CallToolResult {
    val id = arguments.stringArg("id") ?: return errorResult("Missing required argument 'id'.")
    if (!id.startsWith(MCP_ID_PREFIX)) {
        return errorResult("Refusing to delete '$id': id must start with '$MCP_ID_PREFIX'.")
    }
    store.delete(id)
    return ok("Deleted workflow '$id'.")
}

internal suspend fun executeWorkflow(
    store: WorkflowStore,
    runtime: GraphynServerRuntime,
    registry: GraphynRunRegistry,
    json: Json,
    arguments: JsonObject?,
): CallToolResult {
    val id = arguments.stringArg("id") ?: return errorResult("Missing required argument 'id'.")
    val stored = store.load(id) ?: return errorResult("No workflow found with id '$id'.")
    val overridesJson = arguments?.get("overrides") as? JsonObject
    val overrides = overridesJson
        ?.let { json.decodeFromJsonElement<Map<String, Map<String, WorkflowValue>>>(it) }
        ?: emptyMap()
    val workflow = applyOverrides(stored, overrides)
    val errors = runtime.validator.validate(workflow)
    if (errors.isNotEmpty()) return errorResult("Validation failed: ${json.encodeToString(errors)}")

    val async = arguments?.get("async")?.jsonPrimitive?.booleanOrNull ?: false
    if (async) {
        if (!registry.canAcceptRun) return errorResult("Server is at capacity — try again later.")
        val runId = registry.start(workflow)
        return ok(json.encodeToString(RunStarted(runId)))
    }
    val result = runtime.executionEngine.execute(workflow)
    return ok(json.encodeToString(result))
}

internal suspend fun executionStatus(registry: GraphynRunRegistry, json: Json, arguments: JsonObject?): CallToolResult {
    val runId = arguments.stringArg("runId") ?: return errorResult("Missing required argument 'runId'.")
    val messages = registry.messages(runId)?.replayCache
        ?: return errorResult("No run found with id '$runId'.")
    return ok(json.encodeToString<List<ExecutionStreamMessage>>(messages))
}

internal suspend fun listNodeTypes(runtime: GraphynServerRuntime, json: Json): CallToolResult =
    ok(json.encodeToString(runtime.plugins.nodeSpecs.all()))

private fun JsonObject?.stringArg(key: String): String? = this?.get(key)?.jsonPrimitive?.content

private fun ok(text: String): CallToolResult = CallToolResult(content = listOf(TextContent(truncate(text))))

private fun errorResult(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = true)

private fun truncate(text: String): String =
    if (text.length <= MAX_RESPONSE_CHARS) text
    else text.take(MAX_RESPONSE_CHARS) + "\n… truncated, ${text.length - MAX_RESPONSE_CHARS} more characters."
