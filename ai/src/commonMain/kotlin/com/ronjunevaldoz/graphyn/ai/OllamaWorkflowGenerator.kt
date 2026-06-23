package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * [WorkflowGenerator] backed by an Ollama host's `/api/generate` endpoint.
 *
 * Sends the prompt with `format=json` and `stream=false` so the model returns a single JSON
 * body, then validates it through [WorkflowJsonParser]. Network/parse failures map to
 * [WorkflowGenerationResult.Failure] with a user-facing message — never throws to the caller.
 */
class OllamaWorkflowGenerator(
    private val config: OllamaConfig = OllamaConfig(),
    private val httpClient: HttpClient = createHttpClient(),
) : WorkflowGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GenerateRequest(
        val model: String,
        val prompt: String,
        val system: String,
        val stream: Boolean = false,
        val format: String = "json",
    )

    @Serializable
    private data class GenerateResponse(val response: String = "")

    override suspend fun generate(prompt: String, catalog: List<NodeSpec>): WorkflowGenerationResult {
        if (prompt.isBlank()) return WorkflowGenerationResult.Failure("Enter a prompt to generate from.")
        val body = json.encodeToString(
            GenerateRequest(
                model = config.model,
                prompt = WorkflowGenerationPrompt.user(prompt),
                system = WorkflowGenerationPrompt.system(catalog),
            ),
        )
        val text = try {
            val response = httpClient.post(config.generateUrl) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return WorkflowGenerationResult.Failure("Ollama returned HTTP ${response.status.value}.")
            }
            // Ollama may return a single JSON object or NDJSON (one frame per line) even with
            // stream=false behind a proxy. Concatenate every frame's `response` field for both cases.
            response.bodyAsText().lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line -> runCatching { json.decodeFromString<GenerateResponse>(line).response }.getOrNull() }
                .joinToString("")
        } catch (e: Exception) {
            return WorkflowGenerationResult.Failure("Could not reach Ollama at ${config.baseUrl}: ${e.message}")
        }
        return WorkflowJsonParser.parse(text, catalog, fallbackId = "wf-${Random.nextLong().and(0xFFFFFFFFL)}")
    }
}
