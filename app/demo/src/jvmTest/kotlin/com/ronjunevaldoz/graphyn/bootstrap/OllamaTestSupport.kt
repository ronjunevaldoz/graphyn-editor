package com.ronjunevaldoz.graphyn.bootstrap

/**
 * Ollama on the same GPU keeps its last-used model resident in VRAM for its keep_alive window,
 * which starves the SD server's VRAM budget and made FLUX runs swing from ~30s to ~550s in the
 * exact same config. Call this before any live SD generation test so timings reflect the SD
 * server alone, not whatever Ollama happens to have loaded at the moment.
 */
suspend fun unloadOllamaModels() {
    val psBody = runCatching {
        java.net.URI("https://ron-local-home.duckdns.org/ollama/api/ps").toURL().readText()
    }.getOrNull() ?: return
    Regex("\"model\"\\s*:\\s*\"([^\"]+)\"").findAll(psBody).map { it.groupValues[1] }.toSet().forEach { model ->
        runCatching {
            val conn = java.net.URI("https://ron-local-home.duckdns.org/ollama/api/generate").toURL()
                .openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.write("""{"model":"$model","keep_alive":0}""".toByteArray())
            conn.inputStream.readBytes()
        }
    }
}
