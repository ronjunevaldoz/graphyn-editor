package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/** Resolved connection to the SD server: where to reach it and the bearer key (null = no auth). */
data class SdConnection(val baseUrl: String, val apiKey: String?)

private const val DEFAULT_SD_URL = "https://ron-local-home.duckdns.org/stablediffusion"

/**
 * Resolves the SD server URL and API key, in-app settings first so the credential panel wins over
 * a stale environment. Precedence: [settings] value → env var → built-in default. Blank = unset.
 */
fun resolveSdConnection(settings: GraphynSettings): SdConnection {
    val url = settings.sdServerUrl.ifBlank { null }
        ?: System.getenv("GRAPHYN_SD_SERVER_URL")?.ifBlank { null }
        ?: DEFAULT_SD_URL
    val key = settings.sdApiKey.ifBlank { null }
        ?: System.getenv("GRAPHYN_SD_API_KEY")?.ifBlank { null }
    return SdConnection(baseUrl = url, apiKey = key)
}

/** Adds the bearer header for [conn] when a key is set; no-op otherwise. */
internal fun HttpRequestBuilder.authWith(conn: SdConnection) {
    conn.apiKey?.let { header("Authorization", "Bearer $it") }
}
