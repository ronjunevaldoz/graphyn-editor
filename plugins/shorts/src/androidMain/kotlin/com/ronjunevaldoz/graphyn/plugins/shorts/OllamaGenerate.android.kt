package com.ronjunevaldoz.graphyn.plugins.shorts

/** Android actual: same java.net path as JVM — Ollama is reachable over the local network. */
public actual suspend fun ollamaHttpPost(url: String, body: String): String {
    val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.write(body.toByteArray())
    return conn.inputStream.readBytes().decodeToString()
}
