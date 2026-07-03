package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowTemplate

/**
 * The catalog entries that are actually runnable with [nodeSpecs] — every node type must either
 * resolve to a registered spec or embed a subgraph the engine can execute recursively. This keeps
 * the launcher from advertising templates the current platform can't execute (e.g. the JVM-only
 * `script.eval` and `media.*` flows on Web/JS, where those plugins aren't installed). Entries are
 * returned in category order, then AI badges first, then newer catalog entries before older ones
 * inside each category.
 */
fun catalogTemplatesFor(nodeSpecs: NodeSpecRegistry): List<WorkflowTemplate> =
    WorkflowCatalog.entries
        .sortedWith(
            compareBy<WorkflowCatalog>({ it.category.ordinal }, { badgePriority(it.badges) }, { -it.ordinal }),
        )
        .filter { entry -> entry.workflow.nodes.all { it.subgraph != null || nodeSpecs.resolve(it.type) != null } }
        .map { WorkflowTemplate(it.label, it.description, it.workflow, it.category, it.badges) }

private fun badgePriority(badges: List<String>): Int = when {
    "AI" in badges -> 0
    "Stable" in badges -> 1
    "Demo" in badges -> 2
    else -> 3
}
