package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.ai.OllamaConfig
import com.ronjunevaldoz.graphyn.ai.OllamaWorkflowGenerator
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerationResult
import com.ronjunevaldoz.graphyn.ai.WorkflowGenerator
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.store.FileSettingsStore
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings

/**
 * A [WorkflowGenerator] whose Ollama URL follows the settings, resolved fresh per call (active
 * environment → env var → [fallbackUrl]). The underlying [OllamaWorkflowGenerator] is cached and
 * rebuilt only when the URL changes, so editing the AI URL in the credentials panel applies on the
 * next generation without a restart.
 */
class SettingsBackedWorkflowGenerator(
    private val settingsStore: FileSettingsStore,
    private val fallbackUrl: String = OllamaConfig.DEFAULT_BASE_URL,
) : WorkflowGenerator {
    private val lock = Any()
    private var cachedUrl: String? = null
    private var cached: OllamaWorkflowGenerator? = null

    private fun delegate(): OllamaWorkflowGenerator {
        val url = settingsStore.read().value(GraphynSettings.KEY_AI_URL)
            ?: envValue("ollama_host", "GRAPHYN_OLLAMA_HOST")
            ?: fallbackUrl
        return synchronized(lock) {
            if (url != cachedUrl || cached == null) {
                cached = OllamaWorkflowGenerator(OllamaConfig(baseUrl = url))
                cachedUrl = url
            }
            cached!!
        }
    }

    override suspend fun generate(prompt: String, catalog: List<NodeSpec>): WorkflowGenerationResult =
        delegate().generate(prompt, catalog)
}
