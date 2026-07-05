package com.ronjunevaldoz.graphyn.plugins.shorts

/** Android actual: same java.net path as JVM — Ollama is reachable over the local network. */
public actual suspend fun unloadOllamaModel() {
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
