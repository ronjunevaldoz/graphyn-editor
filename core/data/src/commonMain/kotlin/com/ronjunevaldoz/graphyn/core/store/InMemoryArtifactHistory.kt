package com.ronjunevaldoz.graphyn.core.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Non-persistent [ArtifactHistory] for tests and platforms without file storage. */
class InMemoryArtifactHistory : ArtifactHistory {
    private val records = mutableListOf<ArtifactRecord>()
    private val mutex = Mutex()

    override suspend fun record(record: ArtifactRecord): Unit = mutex.withLock {
        records += record
    }

    override suspend fun list(limit: Int): List<ArtifactRecord> = mutex.withLock {
        records.asReversed().take(limit)
    }

    override suspend fun clear(): Unit = mutex.withLock {
        records.clear()
    }
}
