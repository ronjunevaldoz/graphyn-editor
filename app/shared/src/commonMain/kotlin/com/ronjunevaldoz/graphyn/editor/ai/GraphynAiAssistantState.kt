package com.ronjunevaldoz.graphyn.editor.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerationResult
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/** One exchange in the assistant transcript: the user's prompt and the assistant's outcome. */
data class AiChatTurn(
    val prompt: String,
    val status: AiTurnStatus,
)

/** Outcome of a generation turn, rendered in the transcript. */
sealed class AiTurnStatus {
    data object Pending : AiTurnStatus()
    /** Succeeded; [summary] is a one-line result, [warning] notes anything sanitized (or null). */
    data class Done(val summary: String, val warning: String?) : AiTurnStatus()
    data class Error(val message: String) : AiTurnStatus()
}

/**
 * Holds the AI assistant transcript and runs generation against the editor's node [catalog].
 *
 * On success it hands the generated [WorkflowDefinition] to [onApply] (the editor applies it to
 * the canvas) and records a turn noting any unsupported nodes / dropped connections the parser
 * sanitized away, so the user learns why the result differs from their request.
 */
class GraphynAiAssistantState(
    private val generator: WorkflowGenerator,
    private val catalog: List<NodeSpec>,
    private val onApply: (WorkflowDefinition) -> Unit,
    private val currentWorkflow: () -> WorkflowDefinition? = { null },
    private val validateWorkflow: ((WorkflowDefinition) -> List<ValidationError>)? = null,
) {
    var turns by mutableStateOf<List<AiChatTurn>>(emptyList())
        private set
    var generating by mutableStateOf(false)
        private set

    /** Runs one generation turn for [prompt]; updates the transcript as it progresses. */
    suspend fun submit(prompt: String) {
        if (prompt.isBlank() || generating) return
        generating = true
        turns = turns + AiChatTurn(prompt, AiTurnStatus.Pending)
        val status = try {
            if (isAnalysisPrompt(prompt)) {
                analyzeCurrentWorkflow()
            } else when (val result = generator.generate(prompt, catalog)) {
                is WorkflowGenerationResult.Success -> {
                    onApply(result.workflow)
                    AiTurnStatus.Done(summary = summarize(result), warning = warningOf(result))
                }
                is WorkflowGenerationResult.Failure -> AiTurnStatus.Error(result.message)
            }
        } finally {
            generating = false
        }
        turns = turns.dropLast(1) + AiChatTurn(prompt, status)
    }

    fun clear() { turns = emptyList() }

    private fun summarize(r: WorkflowGenerationResult.Success): String {
        val n = r.workflow.nodes.size
        val c = r.workflow.connections.size
        return "Created $n node${plural(n)}, $c connection${plural(c)}."
    }

    private fun warningOf(r: WorkflowGenerationResult.Success): String? {
        val parts = mutableListOf<String>()
        if (r.droppedNodes.isNotEmpty()) {
            parts.add("Skipped unsupported node${plural(r.droppedNodes.size)}: ${r.droppedNodes.joinToString(", ")}")
        }
        if (r.droppedConnections > 0) {
            parts.add("Dropped ${r.droppedConnections} invalid connection${plural(r.droppedConnections)}")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private fun isAnalysisPrompt(prompt: String): Boolean {
        val text = prompt.trim().lowercase()
        val analysisHints = listOf(
            "analy", "review", "inspect", "explain", "summar", "what does",
            "what is wrong", "what's wrong", "why ", "issue", "problem",
            "refine", "improve", "validate", "audit",
        )
        val generationHints = listOf("build ", "generate ", "create ", "make ")
        return analysisHints.any { text.contains(it) } && generationHints.none { text.contains(it) }
    }

    private fun analyzeCurrentWorkflow(): AiTurnStatus {
        val workflow = currentWorkflow()
            ?: return AiTurnStatus.Error("Open a workflow first, then ask me to analyze it.")
        val errors = validateWorkflow?.invoke(workflow).orEmpty()
        val nodeMix = workflow.nodes
            .groupingBy { it.type }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString(", ") { (type, count) -> "$type ×$count" }

        val summary = buildString {
            append("${workflow.name} has ${workflow.nodes.size} node${plural(workflow.nodes.size)} and ")
            append("${workflow.connections.size} connection${plural(workflow.connections.size)}.")
            if (nodeMix.isNotBlank()) append(" Main node mix: $nodeMix.")
            append(
                if (errors.isEmpty()) " Validator found no structural issues."
                else " Validator found ${errors.size} issue${plural(errors.size)}."
            )
        }
        val warning = errors
            .take(3)
            .joinToString(" · ") { error ->
                buildString {
                    append(error.code)
                    error.nodeId?.let { append(" @ $it") }
                    append(": ${error.message}")
                }
            }
            .takeIf { it.isNotBlank() }
        return AiTurnStatus.Done(summary = summary, warning = warning)
    }

    private fun plural(n: Int) = if (n == 1) "" else "s"
}
