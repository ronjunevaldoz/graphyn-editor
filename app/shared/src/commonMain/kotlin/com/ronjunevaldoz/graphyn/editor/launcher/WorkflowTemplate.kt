package com.ronjunevaldoz.graphyn.editor.launcher

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/**
 * A named, pre-built workflow offered in the launcher as a starting point.
 *
 * Pass a list of these to [GraphynWorkflowLauncher] as the `templates` parameter.
 * Recents are tracked the same way — the launcher does not distinguish between them structurally.
 *
 * @param name Display name shown on the card.
 * @param description Optional one-line hint shown beneath the name.
 * @param workflow The [WorkflowDefinition] loaded when the card is tapped.
 */
data class WorkflowTemplate(
    val name: String,
    val description: String? = null,
    val workflow: WorkflowDefinition,
)
