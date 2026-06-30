package com.ronjunevaldoz.graphyn.core.store

import kotlinx.serialization.Serializable

/** Media kind of a generated artifact, derived from its file extension at record time. */
@Serializable
enum class ArtifactKind { Image, Video, Audio }

/**
 * One generated output, captured so it survives the run that produced it and can be browsed or
 * re-used (e.g. fed back in as an init image) later.
 *
 * @param id stable unique id (the output file name is a good default).
 * @param path absolute path to the output file.
 * @param createdAt epoch milliseconds when the artifact was recorded.
 * @param elapsedMs wall-clock generation time, when known.
 */
@Serializable
data class ArtifactRecord(
    val id: String,
    val kind: ArtifactKind,
    val path: String,
    val createdAt: Long,
    val workflowId: String? = null,
    val workflowName: String? = null,
    val nodeType: String? = null,
    val prompt: String? = null,
    val model: String? = null,
    val elapsedMs: Long? = null,
)

/**
 * Durable, append-only log of generated [ArtifactRecord]s, newest first on read.
 *
 * Separate from [WorkflowStore]: a workflow is the recipe, an artifact is a single produced output.
 * Implementations must be safe for concurrent coroutine access.
 */
interface ArtifactHistory {
    /** Appends [record] to the history. */
    suspend fun record(record: ArtifactRecord)

    /** Returns up to [limit] records, newest first. */
    suspend fun list(limit: Int = DEFAULT_LIMIT): List<ArtifactRecord>

    /** Removes all recorded artifacts (metadata only; does not delete the output files). */
    suspend fun clear()

    companion object {
        const val DEFAULT_LIMIT: Int = 200
    }
}
