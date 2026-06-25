package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * In-memory [WorkflowStore] — state is lost on process restart.
 *
 * Useful for tests and as the default on platforms without a persistent file store
 * (Android, iOS). Thread-safe via [Mutex].
 */
class InMemoryWorkflowStore : WorkflowStore {
    private val mutex = Mutex()
    private val metas = mutableMapOf<String, WorkflowMeta>()
    private val versions = mutableMapOf<String, MutableList<WorkflowVersion>>()

    override suspend fun save(workflow: WorkflowDefinition): WorkflowMeta = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val versionList = versions.getOrPut(workflow.id) { mutableListOf() }
        val diff = versionList.lastOrNull()?.snapshot?.let { prev ->
            WorkflowDiffComputer.compute(prev, workflow)
        }
        val versionId = "${workflow.id}-v${versionList.size + 1}"
        versionList += WorkflowVersion(versionId, workflow.id, now, workflow, diff)
        val meta = metas[workflow.id]?.copy(
            name = workflow.name,
            updatedAt = now,
            versionCount = versionList.size,
        ) ?: WorkflowMeta(
            id = workflow.id,
            name = workflow.name,
            createdAt = now,
            updatedAt = now,
        )
        metas[workflow.id] = meta
        meta
    }

    override suspend fun load(id: String): WorkflowDefinition? = mutex.withLock {
        versions[id]?.lastOrNull()?.snapshot
    }

    override suspend fun list(): List<WorkflowMeta> = mutex.withLock {
        metas.values.sortedByDescending { it.updatedAt }
    }

    override suspend fun delete(id: String): Unit = mutex.withLock {
        metas.remove(id)
        versions.remove(id)
    }

    override suspend fun history(id: String): List<WorkflowVersion> = mutex.withLock {
        versions[id]?.reversed() ?: emptyList()
    }

    override suspend fun markExecuted(id: String): Unit = mutex.withLock {
        metas[id]?.let {
            metas[id] = it.copy(lastExecutedAt = Clock.System.now().toEpochMilliseconds())
        }
    }
}
