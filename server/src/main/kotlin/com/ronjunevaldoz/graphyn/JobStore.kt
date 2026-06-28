package com.ronjunevaldoz.graphyn

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for [WorkflowJob] entries and their backing coroutine handles.
 *
 * The coroutine handle is kept separately from the serializable [WorkflowJob] so the model
 * stays clean. Swap this class for a database-backed implementation to persist jobs across restarts.
 */
class JobStore {
    private val jobs = ConcurrentHashMap<String, WorkflowJob>()
    private val handles = ConcurrentHashMap<String, Job>()

    fun add(job: WorkflowJob, handle: Job) {
        jobs[job.id] = job
        handles[job.id] = handle
    }

    fun get(id: String): WorkflowJob? = jobs[id]

    fun all(state: JobState? = null): List<WorkflowJob> {
        val sorted = jobs.values.sortedByDescending { it.submittedAt }
        return if (state != null) sorted.filter { it.state == state } else sorted
    }

    fun update(id: String, block: (WorkflowJob) -> WorkflowJob) {
        jobs.computeIfPresent(id) { _, j -> block(j) }
    }

    /** Cancels the coroutine for [id]. Returns false if no handle is registered. */
    fun cancel(id: String): Boolean = handles[id]?.cancel()?.let { true } ?: false
}
