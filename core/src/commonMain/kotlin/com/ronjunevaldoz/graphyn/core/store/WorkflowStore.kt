package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/**
 * Durable storage for [WorkflowDefinition]s with full version history.
 *
 * Every [save] creates a new [WorkflowVersion] and updates [WorkflowMeta].
 * Implementations must be safe for concurrent coroutine access.
 *
 * Typical use:
 * ```kotlin
 * val store: WorkflowStore = FileWorkflowStore()
 * val meta = store.save(workflow)
 * val versions = store.history(workflow.id) // newest first
 * val diff = versions.first().diff          // what changed in the last save
 * ```
 */
interface WorkflowStore {
    /** Persists [workflow], creating a new version. Returns the updated [WorkflowMeta]. */
    suspend fun save(workflow: WorkflowDefinition): WorkflowMeta

    /** Returns the latest snapshot for [id], or null when not found. */
    suspend fun load(id: String): WorkflowDefinition?

    /** Lists metadata for all stored workflows, sorted newest-updated first. */
    suspend fun list(): List<WorkflowMeta>

    /** Removes the workflow and all its versions. No-op when [id] is unknown. */
    suspend fun delete(id: String)

    /** Returns all saved versions for [id], newest first. Empty when [id] is unknown. */
    suspend fun history(id: String): List<WorkflowVersion>

    /** Records the current time as [WorkflowMeta.lastExecutedAt] for [id]. */
    suspend fun markExecuted(id: String)
}
