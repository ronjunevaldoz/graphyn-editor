package com.ronjunevaldoz.graphyn.core.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * User-editable app settings that must survive restarts — the in-app alternative to environment
 * variables, which GUI-launched desktop apps don't reliably inherit. Extend with new service
 * credentials as needed (keep secrets here, never in committed config).
 *
 * @param sdServerUrl base URL of the stable-diffusion server; blank = use the app default.
 * @param sdApiKey bearer token for the SD server; blank = no auth header sent.
 */
@Serializable
data class GraphynSettings(
    val sdServerUrl: String = "",
    val sdApiKey: String = "",
)

/** Durable storage for [GraphynSettings]. Implementations must be safe for concurrent coroutine access. */
interface SettingsStore {
    suspend fun load(): GraphynSettings
    suspend fun save(settings: GraphynSettings)
}

/** Non-persistent [SettingsStore] for tests and platforms without file storage. */
class InMemorySettingsStore(initial: GraphynSettings = GraphynSettings()) : SettingsStore {
    private var settings = initial
    private val mutex = Mutex()
    override suspend fun load(): GraphynSettings = mutex.withLock { settings }
    override suspend fun save(settings: GraphynSettings): Unit = mutex.withLock { this.settings = settings }
}
