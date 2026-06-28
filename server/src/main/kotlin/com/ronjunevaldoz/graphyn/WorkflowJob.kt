package com.ronjunevaldoz.graphyn

import com.ronjunevaldoz.graphyn.core.execution.WorkflowExecutionResult
import kotlinx.serialization.Serializable

/** Lifecycle states a [WorkflowJob] moves through. Terminal states: [COMPLETED], [FAILED], [CANCELLED]. */
@Serializable
enum class JobState { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

/**
 * A persisted async workflow execution unit, returned by `POST /jobs` and queryable via `GET /jobs/{id}`.
 *
 * Unlike a fire-and-forget `/executions` run, a job survives past request completion and is
 * queryable by id until the server restarts.
 */
@Serializable
data class WorkflowJob(
    val id: String,
    val workflowId: String?,
    val state: JobState,
    val submittedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val result: WorkflowExecutionResult? = null,
    val error: String? = null,
)
