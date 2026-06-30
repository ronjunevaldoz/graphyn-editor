package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.registry.NodeSpecRegistry
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowTemplate

/**
 * The catalog entries that are actually runnable with [nodeSpecs] — every node type in the workflow
 * must resolve to a registered spec. This keeps the launcher from advertising templates the current
 * platform can't execute (e.g. the JVM-only `script.eval` and `media.*` flows on Web/JS, where those
 * plugins aren't installed). Entries are returned in catalog (category) order.
 */
fun catalogTemplatesFor(nodeSpecs: NodeSpecRegistry): List<WorkflowTemplate> =
    WorkflowCatalog.entries
        .sortedBy { it.category.ordinal }
        .filter { entry -> entry.workflow.nodes.all { nodeSpecs.resolve(it.type) != null } }
        .map { WorkflowTemplate(it.label, it.description, it.workflow, it.category, it.badge) }
