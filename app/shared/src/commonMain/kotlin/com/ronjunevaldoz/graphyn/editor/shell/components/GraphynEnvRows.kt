package com.ronjunevaldoz.graphyn.editor.shell.components

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings
import com.ronjunevaldoz.graphyn.core.store.canonicalSettingKey
import com.ronjunevaldoz.graphyn.core.store.settingValue

/** One editable key/value row. [pinned] rows keep the built-in defaults visible but still editable. */
internal data class EnvRow(val key: String, val value: String, val pinned: Boolean, val label: String = "")

private val KNOWN_KEYS = listOf(
    GraphynSettings.KEY_SD_URL to "SD Server URL",
    GraphynSettings.KEY_SD_API_KEY to "SD API Key",
    GraphynSettings.KEY_AI_URL to "AI Assistant URL",
)

/** Editable rows for [envName]: the three well-known keys first, then any custom keys. */
internal fun rowsForEnv(settings: GraphynSettings, envName: String): List<EnvRow> {
    val values = settings.environments.firstOrNull { it.name == envName }?.values.orEmpty()
    val known = KNOWN_KEYS.map { (key, label) -> EnvRow(key, values.settingValue(key).orEmpty(), pinned = true, label = label) }
    val custom = values.filterKeys { k -> KNOWN_KEYS.none { canonicalSettingKey(it.first) == canonicalSettingKey(k) } }
        .map { (k, v) -> EnvRow(canonicalSettingKey(k), v, pinned = false) }
    return known + custom
}

/** Folds [rows] back into [envName] within [settings] (blank keys dropped), returning updated settings. */
internal fun foldRows(settings: GraphynSettings, envName: String, rows: List<EnvRow>): GraphynSettings {
    val values = rows.filter { it.key.isNotBlank() }.associate { canonicalSettingKey(it.key.trim()) to it.value }
    val envs = settings.environments.toMutableList()
    val idx = envs.indexOfFirst { it.name == envName }
    if (idx >= 0) envs[idx] = envs[idx].copy(values = values) else envs += GraphynEnvironment(envName, values)
    return settings.copy(environments = envs)
}

/** Adds an empty environment named [name] (no-op if blank or already present) and makes it active. */
internal fun addEnv(settings: GraphynSettings, name: String): GraphynSettings {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || settings.environments.any { it.name == trimmed }) return settings
    return settings.copy(
        environments = settings.environments + GraphynEnvironment(trimmed),
        activeEnvironment = trimmed,
    )
}

internal const val RUNPOD_ENV = "runpod"

/**
 * Scaffolds a "runpod" environment pointing at a RunPod serverless load-balancing endpoint, and
 * makes it active. Pre-fills the URL template (user replaces `ENDPOINT_ID`) and leaves the SD API
 * key blank. If the environment already exists it's just activated, preserving the user's values.
 */
internal fun addRunPodEnv(settings: GraphynSettings): GraphynSettings {
    if (settings.environments.any { it.name == RUNPOD_ENV }) {
        return settings.copy(activeEnvironment = RUNPOD_ENV)
    }
    val values = mapOf(
        GraphynSettings.KEY_SD_URL to "https://ENDPOINT_ID.api.runpod.ai",
        GraphynSettings.KEY_SD_API_KEY to "",
    )
    return settings.copy(
        environments = settings.environments + GraphynEnvironment(RUNPOD_ENV, values),
        activeEnvironment = RUNPOD_ENV,
    )
}
