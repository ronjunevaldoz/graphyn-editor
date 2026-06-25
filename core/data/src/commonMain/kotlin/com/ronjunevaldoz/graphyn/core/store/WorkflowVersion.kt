package com.ronjunevaldoz.graphyn.core.store

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import kotlinx.serialization.Serializable

/**
 * A point-in-time snapshot of a [WorkflowDefinition] produced on every auto-save.
 *
 * [diff] describes what changed from the immediately preceding version. It is null
 * for the very first version (no predecessor to diff against).
 */
@Serializable
data class WorkflowVersion(
    val versionId: String,
    val workflowId: String,
    val savedAt: Long,
    val snapshot: WorkflowDefinition,
    val diff: WorkflowDiff? = null,
)
