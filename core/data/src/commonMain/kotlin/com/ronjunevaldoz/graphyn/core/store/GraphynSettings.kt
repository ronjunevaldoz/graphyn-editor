package com.ronjunevaldoz.graphyn.core.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * A named group of key→value settings — a profile like "dev" or "prod". Well-known keys
 * (see [GraphynSettings]) drive built-in services; any other keys are user-defined.
 */
@Serializable
data class GraphynEnvironment(
    val name: String,
    val values: Map<String, String> = emptyMap(),
)

/**
 * User-editable app settings that survive restarts — the in-app alternative to environment
 * variables, which GUI-launched desktop apps don't reliably inherit.
 *
 * Values are grouped into named [environments]; the [activeEnvironment] one is what resolves.
 * Look up a key with [value] (falls back to the legacy top-level fields for migration).
 */
@Serializable
data class GraphynSettings(
    val activeEnvironment: String = DEFAULT_ENV,
    val environments: List<GraphynEnvironment> = listOf(GraphynEnvironment(DEFAULT_ENV)),
    // Legacy pre-environments fields; still read by [value] so old settings.json keeps working.
    val sdServerUrl: String = "",
    val sdApiKey: String = "",
) {
    /** Values of the [activeEnvironment] (empty when it doesn't exist). */
    fun activeValues(): Map<String, String> =
        environments.firstOrNull { it.name == activeEnvironment }?.values.orEmpty()

    /** Active-environment value for [key], then the legacy field for well-known keys, else null. */
    fun value(key: String): String? = activeValues()[key]?.ifBlank { null } ?: when (key) {
        KEY_SD_URL -> sdServerUrl.ifBlank { null }
        KEY_SD_API_KEY -> sdApiKey.ifBlank { null }
        else -> null
    }

    /** Folds legacy top-level fields into the active environment (existing values win) and clears them. */
    fun migrated(): GraphynSettings {
        val legacy = buildMap {
            if (sdServerUrl.isNotBlank()) put(KEY_SD_URL, sdServerUrl)
            if (sdApiKey.isNotBlank()) put(KEY_SD_API_KEY, sdApiKey)
        }
        val active = activeEnvironment.ifBlank { DEFAULT_ENV }
        val base = environments.ifEmpty { listOf(GraphynEnvironment(active)) }
        if (legacy.isEmpty()) return copy(activeEnvironment = active, environments = base)
        val envs = base.map { if (it.name == active) it.copy(values = legacy + it.values) else it }
        return GraphynSettings(active, envs)
    }

    companion object {
        const val DEFAULT_ENV = "default"
        const val KEY_SD_URL = "GRAPHYN_SD_SERVER_URL"
        const val KEY_SD_API_KEY = "GRAPHYN_SD_API_KEY"
        const val KEY_AI_URL = "GRAPHYN_OLLAMA_HOST"
    }
}

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
