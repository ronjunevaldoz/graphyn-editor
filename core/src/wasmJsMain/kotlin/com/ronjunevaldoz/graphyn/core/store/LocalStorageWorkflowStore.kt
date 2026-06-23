package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val storeJson = Json { encodeDefaults = false; ignoreUnknownKeys = true }

private const val META_PREFIX = "graphyn.meta."
private const val VERSIONS_PREFIX = "graphyn.versions."

@JsFun("(key) => localStorage.getItem(key)")
private external fun lsGetItem(key: String): String?

@JsFun("(key, value) => { localStorage.setItem(key, value); }")
private external fun lsSetItem(key: String, value: String)

@JsFun("(key) => { localStorage.removeItem(key); }")
private external fun lsRemoveItem(key: String)

@JsFun("() => localStorage.length")
private external fun lsLength(): Int

@JsFun("(index) => localStorage.key(index)")
private external fun lsKey(index: Int): String?

/**
 * [WorkflowStore] backed by the browser's `localStorage` for Kotlin/Wasm targets.
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
        lsSetItem(VERSIONS_PREFIX + workflow.id, storeJson.encodeToString(ListSerializer(WorkflowVersion.serializer()), updated))

        val existingMeta = loadMeta(workflow.id)
        val meta = existingMeta?.copy(
            name = workflow.name,
            updatedAt = now,
            versionCount = updated.size,
        ) ?: WorkflowMeta(workflow.id, workflow.name, createdAt = now, updatedAt = now)
        lsSetItem(META_PREFIX + workflow.id, storeJson.encodeToString(WorkflowMeta.serializer(), meta))
        return meta
    }

    override suspend fun load(id: String): WorkflowDefinition? =
        loadVersionList(id).lastOrNull()?.snapshot

    override suspend fun list(): List<WorkflowMeta> {
        val keys = (0 until lsLength()).mapNotNull { lsKey(it) }
        return keys
            .filter { it.startsWith(META_PREFIX) }
            .mapNotNull { key -> loadRawMeta(lsGetItem(key)) }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun delete(id: String) {
        lsRemoveItem(META_PREFIX + id)
        lsRemoveItem(VERSIONS_PREFIX + id)
    }

    override suspend fun history(id: String): List<WorkflowVersion> =
        loadVersionList(id).reversed()

    override suspend fun markExecuted(id: String) {
        val meta = loadMeta(id) ?: return
        lsSetItem(
            META_PREFIX + id,
            storeJson.encodeToString(WorkflowMeta.serializer(), meta.copy(lastExecutedAt = Clock.System.now().toEpochMilliseconds())),
        )
    }

    private fun loadRawMeta(json: String?): WorkflowMeta? {
        json ?: return null
        return storeJson.decodeFromString(WorkflowMeta.serializer(), json)
    }

    private fun loadMeta(id: String): WorkflowMeta? =
        loadRawMeta(lsGetItem(META_PREFIX + id))

    private fun loadVersionList(id: String): List<WorkflowVersion> {
        val raw = lsGetItem(VERSIONS_PREFIX + id) ?: return emptyList()
        return storeJson.decodeFromString(ListSerializer(WorkflowVersion.serializer()), raw)
    }
}
