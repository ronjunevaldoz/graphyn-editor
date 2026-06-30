package com.ronjunevaldoz.graphyn.core.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

private val historyJson = Json { encodeDefaults = false; ignoreUnknownKeys = true }

/**
 * File-based [ArtifactHistory] for JVM/Desktop. Records are appended as JSON lines (JSONL) to a
 * single index next to the generated files, so the log survives restarts and stays append-cheap.
 *
 * @param indexFile the JSONL index; defaults to `~/.graphyn/artifacts/history.jsonl`.
 */
class FileArtifactHistory(
    private val indexFile: File = File(System.getProperty("user.home"), ".graphyn/artifacts/history.jsonl"),
) : ArtifactHistory {
    private val mutex = Mutex()

    override suspend fun record(record: ArtifactRecord): Unit = mutex.withLock {
        indexFile.parentFile?.mkdirs()
        indexFile.appendText(historyJson.encodeToString(record) + "\n")
    }

    override suspend fun list(limit: Int): List<ArtifactRecord> = mutex.withLock {
        if (!indexFile.exists()) return emptyList()
        indexFile.readLines()
            .asReversed()
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { historyJson.decodeFromString<ArtifactRecord>(it) }.getOrNull() }
            .take(limit)
            .toList()
    }

    override suspend fun clear(): Unit = mutex.withLock {
        indexFile.delete()
    }
}
