package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Standalone Ollama-unload gate for workflows that generate images/video but don't already run
 * [storyboardValidateExecutor] (which unloads as a side effect). Wire its "gate" output into a
 * scene subgraph's "gate" input to force the unload to complete before generation starts, since a
 * bare `runCatching { unloadOllamaModel() }` with no consumer has no ordering guarantee against
 * the rest of the graph.
 */
public val ollamaUnloadExecutor: NodeExecutor = NodeExecutor {
    runCatching { unloadOllamaModel() }
    mapOf("gate" to WorkflowValue.BooleanValue(true))
}

/**
 * Force-unloads `GRAPHYN_OLLAMA_MODEL` (or the default) from Ollama via `keep_alive: 0`.
 *
 * Only the JVM/Android actuals shell out over HTTP; the js/wasm/ios actuals are no-ops, since those
 * targets never drive local Ollama generation — they exist only so the shorts pipeline builders
 * stay callable from the shared workflow catalog.
 */
public expect suspend fun unloadOllamaModel()
