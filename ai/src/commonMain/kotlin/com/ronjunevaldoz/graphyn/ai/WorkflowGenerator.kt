package com.ronjunevaldoz.graphyn.ai

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/**
 * Generates a [WorkflowDefinition] from a natural-language prompt.
 *
 * Implementations: [OllamaWorkflowGenerator] (calls a local/remote Ollama host) and
 * [PlaceholderWorkflowGenerator] (offline canned output for testing/UI development).
 *
 * The [catalog] tells the generator which node types and ports actually exist, so it can
 * only emit valid nodes. Generation is best-effort — callers should handle [WorkflowGenerationResult.Failure].
 */
interface WorkflowGenerator {
    suspend fun generate(prompt: String, catalog: List<NodeSpec>): WorkflowGenerationResult
}

/** Outcome of a generation attempt. */
sealed class WorkflowGenerationResult {
    /** The model produced a valid workflow. [droppedNodes]/[droppedConnections] note anything sanitized away. */
    data class Success(
        val workflow: WorkflowDefinition,
        val droppedNodes: List<String> = emptyList(),
        val droppedConnections: Int = 0,
    ) : WorkflowGenerationResult()

    /** Generation failed (host unreachable, unparseable output, empty result). [message] is user-facing. */
    data class Failure(val message: String) : WorkflowGenerationResult()
}
