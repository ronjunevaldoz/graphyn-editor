package com.ronjunevaldoz.graphyn.core.store

import kotlinx.serialization.Serializable

/**
 * Lightweight record for a stored workflow — list views and quick lookups use this
 * without loading the full snapshot.
 *
 * @param versionCount total number of saved versions, usable as a change counter.
 * @param lastExecutedAt epoch-millis of the last successful run, or null if never run.
 */
@Serializable
data class WorkflowMeta(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val lastExecutedAt: Long? = null,
    val versionCount: Int = 1,
)
