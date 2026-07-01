package com.ronjunevaldoz.graphyn.core.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

private val settingsJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

/**
 * File-based [SettingsStore] for JVM/Desktop, persisting to `~/.graphyn/settings.json`.
 *
 * A missing or unreadable file yields defaults, so first launch just works. Also exposes a
 * synchronous [read] for non-coroutine call sites (e.g. the SD backend resolving its URL/key
 * before a request).
 */
class FileSettingsStore(
    private val file: File = File(System.getProperty("user.home"), ".graphyn/settings.json"),
) : SettingsStore {
    private val mutex = Mutex()

    /** Synchronous best-effort read; migrates legacy fields and returns defaults when absent/malformed. */
    fun read(): GraphynSettings =
        (file.takeIf { it.isFile }
            ?.let { runCatching { settingsJson.decodeFromString<GraphynSettings>(it.readText()) }.getOrNull() }
            ?: GraphynSettings()).migrated()

    override suspend fun load(): GraphynSettings = mutex.withLock { read() }

    override suspend fun save(settings: GraphynSettings): Unit = mutex.withLock {
        file.parentFile?.mkdirs()
        file.writeText(settingsJson.encodeToString(settings))
    }
}
