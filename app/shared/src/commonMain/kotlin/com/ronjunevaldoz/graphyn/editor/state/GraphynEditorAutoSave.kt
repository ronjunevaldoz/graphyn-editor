package com.ronjunevaldoz.graphyn.editor.state

import com.ronjunevaldoz.graphyn.core.store.WorkflowStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val DEBOUNCE_MS = 1_000L

/**
 * Subscribes [store] to [GraphynEditorState.workflowFlow] and persists every change
 * that arrives after a [DEBOUNCE_MS]-ms quiet period.
 *
 * Called from [GraphynEditorState.init] when a store is supplied.
 */
internal fun GraphynEditorState.initAutoSave(store: WorkflowStore) {
    // Debounce manually: each emission cancels the previous pending save.
    var pendingSaveJob = scope.launch { /* sentinel */ }
    workflowFlow.filterNotNull().onEach { workflow ->
        pendingSaveJob.cancel()
        pendingSaveJob = scope.launch {
            delay(DEBOUNCE_MS)
            store.save(workflow)
        }
    }.launchIn(scope)
}
