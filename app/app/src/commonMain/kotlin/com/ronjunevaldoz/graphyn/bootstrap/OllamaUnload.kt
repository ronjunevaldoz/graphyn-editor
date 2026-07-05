package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val OLLAMA_UNLOAD_NODE_TYPE = "demo.ollama.unload"

/**
 * Standalone Ollama-unload gate for workflows that generate images/video but don't already run
 * [storyboardValidateExecutor] (which unloads as a side effect) — e.g. [regenerateSceneWorkflow].
 * Wire its "gate" output into a scene subgraph's "gate" input to force the unload to complete
 * before generation starts, since a bare `runCatching { unloadOllamaModel() }` with no consumer
 * has no ordering guarantee against the rest of the graph.
 */
internal val ollamaUnloadExecutor = NodeExecutor {
    runCatching { unloadOllamaModel() }
    mapOf("gate" to WorkflowValue.BooleanValue(true))
}

/** Force-unloads [GRAPHYN_OLLAMA_MODEL] (or the default) from Ollama via `keep_alive: 0`. */
internal suspend fun unloadOllamaModel() {
    val host = (System.getenv("GRAPHYN_OLLAMA_HOST") ?: "http://localhost:11434").let {
        if (it.endsWith("/")) it.dropLast(1) else it
    }
    val model = System.getenv("GRAPHYN_OLLAMA_MODEL") ?: "llama3.1"
    val conn = java.net.URI("$host/api/generate").toURL().openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.outputStream.write("""{"model":"$model","keep_alive":0}""".toByteArray())
    conn.inputStream.readBytes()
}
