package com.ronjunevaldoz.graphyn.plugins.io

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

// CIO's implicit default request timeout is too short for local LLM calls (e.g. Ollama generating
// a large structured JSON response can legitimately take well over a minute) — confirmed via a real
// "Request timeout has expired" failure against io.http_request's Ollama call. 5 minutes covers slow
// model responses without masking a genuinely hung connection forever.
internal actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 300_000
        connectTimeoutMillis = 300_000
        socketTimeoutMillis = 300_000
    }
}
