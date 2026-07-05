package com.ronjunevaldoz.graphyn.plugins.shorts

/** Unsupported actual: this target never drives local Ollama generation. Surfaces as ok = false. */
public actual suspend fun ollamaHttpPost(url: String, body: String): String =
    throw UnsupportedOperationException("ollama.generate is not supported on this target")
