package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.io.File

private val storeJson = Json {
    prettyPrint = true
    encodeDefaults = false
    ignoreUnknownKeys = true
}

/**
 * File-based [WorkflowStore] for JVM and Desktop targets.
 *
 * Each workflow occupies a subdirectory under [root]:
 * ```
 * {root}/{workflowId}/meta.json
 * {root}/{workflowId}/versions/{versionId}.json
 * ```
 *
 * @param root Directory under which all workflow data is stored.
 *   Defaults to `~/.graphyn/workflows`.
 */
class FileWorkflowStore(
    root: File = File(System.getProperty("user.home"), ".graphyn/workflows"),
) : WorkflowStore {
    private val dir = root.also { it.mkdirs() }
    private val mutex = Mutex()

    private fun workflowDir(id: String) = File(dir, id)
    private fun metaFile(id: String) = File(workflowDir(id), "meta.json")
    private fun versionsDir(id: String) = File(workflowDir(id), "versions")

    override suspend fun save(workflow: WorkflowDefinition): WorkflowMeta = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val wDir = workflowDir(workflow.id).also { it.mkdirs() }
        val vDir = versionsDir(workflow.id).also { it.mkdirs() }

        val existingMeta = metaFile(workflow.id).takeIf { it.exists() }
            ?.let { storeJson.decodeFromString<WorkflowMeta>(it.readText()) }
        val versionCount = (existingMeta?.versionCount ?: 0) + 1
        val versionId = "${workflow.id}-v$versionCount"

        val prev = load(workflow.id)
        val diff = prev?.let { WorkflowDiffComputer.compute(it, workflow) }
        val version = WorkflowVersion(versionId, workflow.id, now, workflow, diff)
        File(vDir, "$versionId.json").writeText(storeJson.encodeToString(version))

        val meta = existingMeta?.copy(
            name = workflow.name,
            updatedAt = now,
            versionCount = versionCount,
        ) ?: WorkflowMeta(workflow.id, workflow.name, createdAt = now, updatedAt = now)
        metaFile(workflow.id).writeText(storeJson.encodeToString(meta))
        meta
    }

    override suspend fun load(id: String): WorkflowDefinition? {
        val vDir = versionsDir(id)
        if (!vDir.exists()) return null
        return vDir.listFiles()
            ?.maxByOrNull { it.name }
            ?.let { storeJson.decodeFromString<WorkflowVersion>(it.readText()).snapshot }
    }

    override suspend fun list(): List<WorkflowMeta> {
        return dir.listFiles()
            ?.mapNotNull { wDir ->
                val mf = File(wDir, "meta.json")
                if (mf.exists()) storeJson.decodeFromString<WorkflowMeta>(mf.readText()) else null
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    override suspend fun delete(id: String): Unit = mutex.withLock {
        workflowDir(id).deleteRecursively()
    }

    override suspend fun history(id: String): List<WorkflowVersion> {
        val vDir = versionsDir(id)
        if (!vDir.exists()) return emptyList()
        return vDir.listFiles()
            ?.sortedByDescending { it.name }
            ?.map { storeJson.decodeFromString<WorkflowVersion>(it.readText()) }
            ?: emptyList()
    }

    override suspend fun markExecuted(id: String): Unit = mutex.withLock {
        val mf = metaFile(id)
        if (!mf.exists()) return@withLock
        val meta = storeJson.decodeFromString<WorkflowMeta>(mf.readText())
            .copy(lastExecutedAt = Clock.System.now().toEpochMilliseconds())
        mf.writeText(storeJson.encodeToString(meta))
    }
}
