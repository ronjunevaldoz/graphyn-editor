package com.ronjunevaldoz.graphyn.editor.panels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.ronjunevaldoz.graphyn.core.GraphynExperimentalApi
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * All data available to a custom inspector panel at render time.
 *
 * @param workflow The workflow currently open in the editor, or null if no workflow is loaded.
 * @param selectedNode The node the user has selected, or null if nothing is selected.
 * @param selectedNodeSpec The [NodeSpec] for [selectedNode], or null when no node is selected.
 * @param validationErrors All validation problems found in the current workflow.
 * @param selectedNodeOutputs Raw execution outputs for [selectedNode] from the last run.
 * @param flattenedSelectedNodeOutputs Outputs with composite values flattened to a single level.
 * @param onConfigChange Called when the panel edits a node config value.
 * @param onEnterSubgraph Provided when the host supports subgraph navigation; null otherwise.
 */
@Stable
data class EditorPanelContext(
    val workflow: WorkflowDefinition?,
    val selectedNode: NodeRef?,
    val selectedNodeSpec: NodeSpec?,
    val validationErrors: List<ValidationError>,
    val selectedNodeOutputs: Map<String, WorkflowValue>,
    val flattenedSelectedNodeOutputs: Map<String, WorkflowValue>,
    val onConfigChange: (key: String, value: WorkflowValue) -> Unit = { _, _ -> },
    val onEnterSubgraph: ((inner: WorkflowDefinition) -> Unit)? = null,
)

/**
 * Produces a custom inspector panel composable for a node type.
 *
 * Register via [EditorPanelRegistry.register] to replace the default inspector for a given type.
 */
fun interface EditorPanelFactory {
    @Composable
    fun Content(context: EditorPanelContext)
}

/** Maps node type strings to their [EditorPanelFactory] for the inspector panel. */
interface EditorPanelRegistry {
    /** Returns the panel factory registered for [nodeType], or null to use the default inspector. */
    fun resolve(nodeType: String): EditorPanelFactory?
    /** Registers [factory] as the inspector panel for [nodeType]. */
    fun register(nodeType: String, factory: EditorPanelFactory)
}

/** In-memory [EditorPanelRegistry]. */
@GraphynExperimentalApi
class DefaultEditorPanelRegistry : EditorPanelRegistry {
    private val panels = mutableMapOf<String, EditorPanelFactory>()

    override fun resolve(nodeType: String): EditorPanelFactory? = panels[nodeType]

    override fun register(nodeType: String, factory: EditorPanelFactory) {
        panels[nodeType] = factory
    }
}
