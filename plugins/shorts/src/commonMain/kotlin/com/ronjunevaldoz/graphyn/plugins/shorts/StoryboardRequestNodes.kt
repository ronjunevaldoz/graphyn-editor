package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

// Compiled executors for the storyboard pipeline's Ollama request. These replace the script.eval
// nodes an earlier version used: script.eval's shared JSR-223 engine corrupts its own compiler
// state after a few sequential different scripts, crashing later ones even when trivially simple.

/** Builds the `/api/generate` URL from an Ollama host string. */
public val ollamaUrlSpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.OLLAMA_URL, label = "Ollama URL", category = ShortsConstants.CATEGORY,
    description = "Builds the /api/generate URL from an Ollama host string.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)

/** Executor for [ollamaUrlSpec]. */
public val ollamaUrlExecutor: NodeExecutor = NodeExecutor { inputs ->
    val host = ((inputs["input"] as? WorkflowValue.StringValue)?.value?.ifBlank { null } ?: "http://localhost:11434")
        .let { if (it.endsWith("/")) it.dropLast(1) else it }
    mapOf("result" to WorkflowValue.StringValue("$host/api/generate"))
}

/** Builds the Ollama `/api/generate` request body for the storyboard prompt. */
public val ollamaBodySpec: NodeSpec = NodeSpec(
    type = ShortsNodeTypes.OLLAMA_BODY, label = "Ollama Body", category = ShortsConstants.CATEGORY,
    description = "Builds the Ollama /api/generate request body for the storyboard prompt.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("topic", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)

/** Executor for [ollamaBodySpec]. */
public val ollamaBodyExecutor: NodeExecutor = NodeExecutor { inputs ->
    val model = (inputs["input"] as? WorkflowValue.StringValue)?.value?.ifBlank { null } ?: "llama3.1"
    val topic = inputs.stringOr("topic", "")
    mapOf("result" to WorkflowValue.RecordValue(
        mapOf(
            "model" to WorkflowValue.StringValue(model),
            "prompt" to WorkflowValue.StringValue(buildStoryboardPrompt(topic)),
            "stream" to WorkflowValue.BooleanValue(false),
            "format" to WorkflowValue.StringValue("json"),
            // Belt-and-suspenders with unloadOllamaModel()'s follow-up call: that call is wrapped in
            // runCatching and silently swallows failures, so if it ever doesn't fire, this makes
            // Ollama drop the model itself right after answering instead of holding it for the
            // default 5-minute keep-alive while server-sd's Flux scenes run on the same GPU.
            "keep_alive" to WorkflowValue.IntValue(0),
        ),
    ))
}
