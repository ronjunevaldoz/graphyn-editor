package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/** Resolved connection to the SD server: where to reach it and the bearer key (null = no auth). */
data class SdConnection(val baseUrl: String, val apiKey: String?)

private const val DEFAULT_SD_URL = "http://127.0.0.1:5000"

internal fun envValue(vararg names: String): String? =
    names.asSequence().mapNotNull { System.getenv(it)?.ifBlank { null } }.firstOrNull()

/**
 * Resolves the SD server URL and API key, in-app settings first so the credential panel wins over
 * a stale environment. Precedence: [settings] value → env var → built-in default. Blank = unset.
 */
fun resolveSdConnection(settings: GraphynSettings): SdConnection {
    val url = settings.value(GraphynSettings.KEY_SD_URL)
        ?: envValue("sd_server_url", "GRAPHYN_SD_SERVER_URL")
        ?: DEFAULT_SD_URL
    val key = settings.value(GraphynSettings.KEY_SD_API_KEY)
        ?: envValue("sd_api_key", "GRAPHYN_SD_API_KEY")
    return SdConnection(baseUrl = url.trimEnd('/'), apiKey = key)
}

/** Adds the bearer header for [conn] when a key is set; no-op otherwise. */
internal fun HttpRequestBuilder.authWith(conn: SdConnection) {
    conn.apiKey?.let { header("Authorization", "Bearer $it") }
}
