package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowTemplate

/**
 * The catalog entries that are actually runnable with [nodeSpecs] — every node type in the workflow
 * must resolve to a registered spec. This keeps the launcher from advertising templates the current
 * platform can't execute (e.g. the JVM-only `script.eval` and `media.*` flows on Web/JS, where those
 * plugins aren't installed). Entries are returned in category order, then AI badges first, then
 * newer catalog entries before older ones inside each category.
 */
fun catalogTemplatesFor(nodeSpecs: NodeSpecRegistry): List<WorkflowTemplate> =
    WorkflowCatalog.entries
        .sortedWith(
            compareBy<WorkflowCatalog>({ it.category.ordinal }, { badgePriority(it.badge) }, { -it.ordinal }),
        )
        .filter { entry -> entry.workflow.nodes.all { nodeSpecs.resolve(it.type) != null } }
        .map { WorkflowTemplate(it.label, it.description, it.workflow, it.category, it.badge) }

private fun badgePriority(badge: String?): Int = when (badge) {
    "AI" -> 0
    "Stable" -> 1
    "Demo" -> 2
    null -> 3
    else -> 4
}
