package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Bare, standalone Ollama text-generation node — the self-contained equivalent of Studio's
 * `studio.generate-script`. Given a `prompt` (and optional `model`), it POSTs to Ollama's
 * `/api/generate` and returns the model's raw `response` text plus the `ok` flag.
 *
 * Unlike [storyboardGeneratorSubgraph] (which wires `env.read` → [ollamaUrlExecutor] →
 * [ollamaBodyExecutor] → `io.http_request` → `json.*` → [storyboardValidateExecutor] as a package
 * deal), this node does the whole call in one executor with no storyboard-specific prompt, no JSON
 * schema, and no validation/fallback. It is purely additive: the storyboard pipeline is unchanged.
 *
 * ```kotlin
 * registrar.registerExecutor(ShortsNodeTypes.OLLAMA_GENERATE, ollamaGenerateExecutor)
 * ```
 */
public val ollamaGenerateSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.OLLAMA_GENERATE,
    label = "Ollama Generate",
    category = ShortsConstants.CATEGORY,
    description = "Calls Ollama /api/generate with a prompt and returns the raw model text.",
    inputs = listOf(
        PortSpec("prompt", WorkflowType.StringType, description = "Prompt / topic sent to the LLM"),
        PortSpec("model", WorkflowType.StringType, required = false, description = "Ollama model name (defaults to GRAPHYN_OLLAMA_MODEL or llama3.1)"),
        PortSpec("host", WorkflowType.StringType, required = false, description = "Ollama host base URL (defaults to GRAPHYN_OLLAMA_HOST or http://localhost:11434)"),
    ),
    outputs = listOf(
        PortSpec("response", WorkflowType.StringType, description = "Raw text the model generated"),
        PortSpec("ok", WorkflowType.BooleanType, description = "True if the call succeeded and returned a response"),
    ),
    defaultValues = mapOf("prompt" to WorkflowValue.StringValue("")),
)

private const val OLLAMA_DEFAULT_HOST: String = "http://localhost:11434"
private const val OLLAMA_DEFAULT_MODEL: String = "llama3.1"

private val ollamaJson = Json { ignoreUnknownKeys = true }

/** Normalizes an Ollama host string to the `/api/generate` endpoint, matching [ollamaUrlExecutor]. */
internal fun ollamaGenerateUrl(host: String?): String {
    val base = (host?.ifBlank { null } ?: OLLAMA_DEFAULT_HOST).let { if (it.endsWith("/")) it.dropLast(1) else it }
    return "$base/api/generate"
}

/** Builds the non-streaming `/api/generate` request body for a free-form [prompt]. */
internal fun ollamaGenerateBody(prompt: String, model: String): String = buildJsonObject {
    put("model", JsonPrimitive(model))
    put("prompt", JsonPrimitive(prompt))
    put("stream", JsonPrimitive(false))
    // Matches ollamaBodyExecutor: drop the model right after answering so it doesn't hold the GPU.
    put("keep_alive", JsonPrimitive(0))
}.toString()

/**
 * Extracts the `response` field from Ollama's non-streaming `/api/generate` JSON. Returns an empty
 * string on any malformed/missing shape rather than throwing — same defensive spirit as
 * [storyboardValidateExecutor], so a bad payload degrades to `ok = false` instead of a crash.
 */
internal fun parseOllamaResponse(raw: String): String? = runCatching {
    (ollamaJson.parseToJsonElement(raw) as? JsonObject)?.get("response")?.jsonPrimitive?.contentOrNull
}.getOrNull()

/**
 * Executor for [ollamaGenerateSpec], built from an injectable [transport] (POST url+body → raw body)
 * so common tests can drive it without a live server. Production wires the platform [ollamaHttpPost].
 */
public fun ollamaGenerateExecutor(
    transport: suspend (url: String, body: String) -> String = ::ollamaHttpPost,
): NodeExecutor = NodeExecutor { inputs ->
    val prompt = inputs.stringOr("prompt", "")
    val model = inputs.stringOr("model", "").ifBlank { OLLAMA_DEFAULT_MODEL }
    val host = inputs.stringOr("host", "").ifBlank { null }
    val result = runCatching { transport(ollamaGenerateUrl(host), ollamaGenerateBody(prompt, model)) }
        .mapCatching { parseOllamaResponse(it) }
        .getOrNull()
    mapOf(
        "response" to WorkflowValue.StringValue(result.orEmpty()),
        "ok" to WorkflowValue.BooleanValue(result != null),
    )
}

/** Default production executor for [ShortsNodeTypes.OLLAMA_GENERATE]. */
public val ollamaGenerateExecutor: NodeExecutor = ollamaGenerateExecutor()

/**
 * Platform HTTP transport: POSTs [body] (JSON) to [url] and returns the raw response body as text.
 *
 * Only the JVM/Android actuals shell out over HTTP; the js/wasm/ios actuals throw, since those
 * targets never drive local Ollama generation — the node stays *defined* everywhere so the shared
 * catalog is uniform, but a call on an unsupported target surfaces as `ok = false`.
 */
public expect suspend fun ollamaHttpPost(url: String, body: String): String
