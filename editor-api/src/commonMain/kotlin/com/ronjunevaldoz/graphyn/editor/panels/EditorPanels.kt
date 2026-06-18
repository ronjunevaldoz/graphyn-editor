package com.ronjunevaldoz.graphyn.editor.panels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Stable
data class EditorPanelContext(
    val workflow: WorkflowDefinition?,
    val selectedNode: NodeRef?,
    val selectedNodeSpec: NodeSpec?,
    val validationErrors: List<ValidationError>,
    val selectedNodeOutputs: Map<String, WorkflowValue>,
    val flattenedSelectedNodeOutputs: Map<String, WorkflowValue>,
    val onConfigChange: (key: String, value: WorkflowValue) -> Unit = { _, _ -> },
)

fun interface EditorPanelFactory {
    @Composable
    fun Content(context: EditorPanelContext)
}

interface EditorPanelRegistry {
    fun resolve(nodeType: String): EditorPanelFactory?
    fun register(nodeType: String, factory: EditorPanelFactory)
}

@GraphynExperimentalApi
class DefaultEditorPanelRegistry : EditorPanelRegistry {
    private val panels = mutableMapOf<String, EditorPanelFactory>()

    override fun resolve(nodeType: String): EditorPanelFactory? = panels[nodeType]

    override fun register(nodeType: String, factory: EditorPanelFactory) {
        panels[nodeType] = factory
    }
}
