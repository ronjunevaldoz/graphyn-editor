package com.ronjunevaldoz.graphyn.editor.state

import kotlin.concurrent.Volatile

/**
 * The workflow currently being executed, so the SD backend can attribute recorded artifacts to it.
 *
 * A node executor only receives its input map, not the workflow, so this thin global carries the
 * run's identity across the execute → executor → backend boundary. Set by [GraphynEditorState.execute]
 * around a run (and cleared after); read when an artifact is recorded. Single-run granularity —
 * adequate for the desktop app where one workflow runs at a time.
 */
object SdArtifactContext {
    @Volatile
    var workflowId: String? = null

    @Volatile
    var workflowName: String? = null

    inline fun <T> withWorkflow(id: String?, name: String?, block: () -> T): T {
        workflowId = id
        workflowName = name
        return try {
            block()
        } finally {
            workflowId = null
            workflowName = null
        }
    }
}
