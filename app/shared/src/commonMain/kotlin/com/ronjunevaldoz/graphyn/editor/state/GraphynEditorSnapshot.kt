package com.ronjunevaldoz.graphyn.editor.state

import androidx.compose.ui.unit.IntOffset
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition

internal data class GraphynEditorSnapshot(
    val workflow: WorkflowDefinition?,
    val positions: Map<String, IntOffset>,
)
