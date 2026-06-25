package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.browser.localStorage
import kotlin.time.Clock
import kotlinx.serialization.json.Json

private val storeJson = Json { encodeDefaults = false; ignoreUnknownKeys = true }

private const val META_PREFIX = "graphyn.meta."
private const val VERSIONS_PREFIX = "graphyn.versions."

/**
 * [WorkflowStore] backed by the browser's [localStorage].
 *
 * Keys used:
 * - `graphyn.meta.{id}` → serialized [WorkflowMeta]
 * - `graphyn.versions.{id}` → JSON array of [WorkflowVersion], newest last
 *
 * Storage is limited to ~5 MB in most browsers. For large workflows with many
 * versions, prefer trimming old versions via a max-history policy.
 *
 * @param maxVersionsPerWorkflow Oldest versions are evicted when the list exceeds this.
 */
class LocalStorageWorkflowStore(
    private val maxVersionsPerWorkflow: Int = 50,
) : WorkflowStore {

    override suspend fun save(workflow: WorkflowDefinition): WorkflowMeta {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = loadVersionList(workflow.id)
        val prev = existing.lastOrNull()?.snapshot
        val diff = prev?.let { WorkflowDiffComputer.compute(it, workflow) }
        val versionId = "${workflow.id}-v${existing.size + 1}"
        val newVersion = WorkflowVersion(versionId, workflow.id, now, workflow, diff)
        val updated = (existing + newVersion).takeLast(maxVersionsPerWorkflow)
        localStorage.setItem(VERSIONS_PREFIX + workflow.id, storeJson.encodeToString(updated))

        val existingMeta = loadMeta(workflow.id)
        val meta = existingMeta?.copy(
            name = workflow.name,
            updatedAt = now,
            versionCount = updated.size,
        ) ?: WorkflowMeta(workflow.id, workflow.name, createdAt = now, updatedAt = now)
        localStorage.setItem(META_PREFIX + workflow.id, storeJson.encodeToString(meta))
        return meta
    }

    override suspend fun load(id: String): WorkflowDefinition? =
        loadVersionList(id).lastOrNull()?.snapshot

    override suspend fun list(): List<WorkflowMeta> {
        val keys = (0 until localStorage.length).mapNotNull { localStorage.key(it) }
        return keys.filter { it.startsWith(META_PREFIX) }
            .mapNotNull { key ->
                localStorage.getItem(key)?.let { storeJson.decodeFromString<WorkflowMeta>(it) }
            }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun delete(id: String) {
        localStorage.removeItem(META_PREFIX + id)
        localStorage.removeItem(VERSIONS_PREFIX + id)
    }

    override suspend fun history(id: String): List<WorkflowVersion> =
        loadVersionList(id).reversed()

    override suspend fun markExecuted(id: String) {
        val meta = loadMeta(id) ?: return
        localStorage.setItem(
            META_PREFIX + id,
            storeJson.encodeToString(meta.copy(lastExecutedAt = Clock.System.now().toEpochMilliseconds())),
        )
    }

    private fun loadMeta(id: String): WorkflowMeta? =
        localStorage.getItem(META_PREFIX + id)?.let { storeJson.decodeFromString<WorkflowMeta>(it) }

    private fun loadVersionList(id: String): List<WorkflowVersion> =
        localStorage.getItem(VERSIONS_PREFIX + id)
            ?.let { storeJson.decodeFromString<List<WorkflowVersion>>(it) }
            ?: emptyList()
}
