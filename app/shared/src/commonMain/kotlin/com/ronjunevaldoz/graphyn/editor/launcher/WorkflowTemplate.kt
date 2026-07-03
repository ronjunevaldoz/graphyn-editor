package com.ronjunevaldoz.graphyn.editor.launcher

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

/**
 * Catalog grouping for templates shown in [GraphynWorkflowLauncher]. Declaration order is the order
 * sections appear in the launcher, so production categories precede learning examples.
 *
 * @param label Section header shown above the group's cards.
 */
enum class WorkflowCategory(val label: String) {
    Media("Media"),
    DataAndIo("Data & IO"),
    Examples("Examples"),
}

/**
 * A named, pre-built workflow offered in the launcher as a starting point.
 *
 * Pass a list of these to [GraphynWorkflowLauncher] as the `templates` parameter.
 * Recents are tracked the same way — the launcher does not distinguish between them structurally.
 *
 * @param name Display name shown on the card.
 * @param description Optional one-line hint shown beneath the name.
 * @param workflow The [WorkflowDefinition] loaded when the card is tapped.
 * @param category Catalog section this template belongs to; defaults to [WorkflowCategory.Examples].
 * @param badges Short labels rendered as colored pills next to the name (e.g. "AI", "Stable", "60% Coverage").
 */
data class WorkflowTemplate(
    val name: String,
    val description: String? = null,
    val workflow: WorkflowDefinition,
    val category: WorkflowCategory = WorkflowCategory.Examples,
    val badges: List<String> = emptyList(),
)
