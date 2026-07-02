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

/** Maps a legacy env key to the canonical snake_case key the editor now stores. */
fun canonicalSettingKey(key: String): String = when (key) {
    LEGACY_SD_URL -> GraphynSettings.KEY_SD_URL
    LEGACY_SD_API_KEY -> GraphynSettings.KEY_SD_API_KEY
    LEGACY_AI_URL -> GraphynSettings.KEY_AI_URL
    else -> key
}

/** Reads a setting by canonical key while still honoring legacy key aliases. */
fun Map<String, String>.settingValue(key: String): String? =
    sequenceOf(key, canonicalSettingKey(key), legacySettingKey(key))
        .distinct()
        .mapNotNull { get(it)?.ifBlank { null } }
        .firstOrNull()

internal fun Map<String, String>.normalizedSettingKeys(): Map<String, String> {
    val normalized = linkedMapOf<String, String>()
    for ((key, value) in this) {
        val canonical = canonicalSettingKey(key)
        if (canonical !in normalized || key == canonical) normalized[canonical] = value
    }
    return normalized
}

private fun legacySettingKey(key: String): String? = when (canonicalSettingKey(key)) {
    GraphynSettings.KEY_SD_URL -> LEGACY_SD_URL
    GraphynSettings.KEY_SD_API_KEY -> LEGACY_SD_API_KEY
    GraphynSettings.KEY_AI_URL -> LEGACY_AI_URL
    else -> null
}

private const val LEGACY_SD_URL = "GRAPHYN_SD_SERVER_URL"
private const val LEGACY_SD_API_KEY = "GRAPHYN_SD_API_KEY"
private const val LEGACY_AI_URL = "GRAPHYN_OLLAMA_HOST"

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
    fun value(key: String): String? = activeValues().settingValue(key) ?: when (canonicalSettingKey(key)) {
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
        val base = environments.map { it.copy(values = it.values.normalizedSettingKeys()) }
            .ifEmpty { listOf(GraphynEnvironment(active)) }
        if (legacy.isEmpty()) return copy(activeEnvironment = active, environments = base)
        val envs = base.map { if (it.name == active) it.copy(values = legacy + it.values) else it }
        return GraphynSettings(active, envs)
    }

    companion object {
        const val DEFAULT_ENV = "default"
        const val KEY_SD_URL = "sd_server_url"
        const val KEY_SD_API_KEY = "sd_api_key"
        const val KEY_AI_URL = "ollama_host"
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
