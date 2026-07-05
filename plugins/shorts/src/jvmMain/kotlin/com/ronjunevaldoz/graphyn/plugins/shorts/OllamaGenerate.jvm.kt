package com.ronjunevaldoz.graphyn.plugins.shorts

/** JVM actual: POSTs the JSON [body] to [url] over java.net and returns the raw response text. */
public actual suspend fun ollamaHttpPost(url: String, body: String): String {
    val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.write(body.toByteArray())
    return conn.inputStream.readBytes().decodeToString()
}
