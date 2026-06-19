package com.ronjunevaldoz.graphyn.editor.state

import kotlin.random.Random

/** Editor-only visual grouping. Not persisted in [WorkflowDefinition]. */
data class NodeGroup(
    val id: String = Random.nextLong().toString(16),
    val label: String = "Group",
    val nodeIds: Set<String>,
)
