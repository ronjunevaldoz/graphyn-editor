package com.ronjunevaldoz.graphyn.ai

/**
 * Connection settings for an Ollama host.
 *
 * @param baseUrl Host root, e.g. `http://localhost:11434` or `https://example.com/ollama`.
 *   A trailing slash is tolerated. The generator appends `/api/generate`.
 * @param model Model tag to use, e.g. `qwen2.5-coder:14b`. Pick a model good at structured JSON.
 * @param timeoutMs Per-request timeout; generation on large models can be slow.
 */
data class OllamaConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val timeoutMs: Long = 120_000,
) {
    val generateUrl: String get() = baseUrl.trimEnd('/') + "/api/generate"

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:11434"
        const val DEFAULT_MODEL = "qwen2.5-coder:14b"
    }
}
