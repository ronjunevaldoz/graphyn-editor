package com.ronjunevaldoz.graphyn.editor.shell.components

import com.ronjunevaldoz.graphyn.core.store.GraphynEnvironment
import com.ronjunevaldoz.graphyn.core.store.GraphynSettings

/** One editable key/value row. [known] rows show a friendly label and a fixed key; custom rows are free-form. */
internal data class EnvRow(val key: String, val value: String, val known: Boolean, val label: String = key)

private val KNOWN_KEYS = listOf(
    GraphynSettings.KEY_SD_URL to "SD Server URL",
    GraphynSettings.KEY_SD_API_KEY to "SD API Key",
    GraphynSettings.KEY_AI_URL to "AI Assistant URL",
)

/** Editable rows for [envName]: the three well-known keys first, then any custom keys. */
internal fun rowsForEnv(settings: GraphynSettings, envName: String): List<EnvRow> {
    val values = settings.environments.firstOrNull { it.name == envName }?.values.orEmpty()
    val known = KNOWN_KEYS.map { (key, label) -> EnvRow(key, values[key].orEmpty(), known = true, label = label) }
    val custom = values.filterKeys { k -> KNOWN_KEYS.none { it.first == k } }
        .map { (k, v) -> EnvRow(k, v, known = false) }
    return known + custom
}

/** Folds [rows] back into [envName] within [settings] (blank keys dropped), returning updated settings. */
internal fun foldRows(settings: GraphynSettings, envName: String, rows: List<EnvRow>): GraphynSettings {
    val values = rows.filter { it.key.isNotBlank() }.associate { it.key to it.value }
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
