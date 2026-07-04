package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.runtime.Composable
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.core.model.NodeGroups
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val SUBGRAPH_NODE_TYPE = "demo.subgraph"
internal const val SHORTS_SCENE_SUBGRAPH_NODE_TYPE = "demo.subgraph.scene"
internal const val SHORTS_BATCH_SUBGRAPH_NODE_TYPE = "demo.subgraph.batch"
internal const val STORYBOARD_SUBGRAPH_NODE_TYPE = "demo.subgraph.storyboard"
internal const val SUBGRAPH_CATEGORY = "demo.composition"

val SubgraphNodeSpec = NodeSpec(
    type = SUBGRAPH_NODE_TYPE,
    label = "Subgraph",
    description = "Encapsulates a nested workflow as a single node",
    category = SUBGRAPH_CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, description = "Data into the subgraph")),
    outputs = listOf(PortSpec("output", WorkflowType.OpaqueType, description = "Data produced by the subgraph")),
)

private val ShortsSceneSubgraphNodeSpec = NodeSpec(
    type = SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
    label = "Scene Subgraph",
    description = "Runs one reusable shorts scene and exposes the rendered video",
    category = SUBGRAPH_CATEGORY,
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, description = "Scene list from the outline"),
        PortSpec("gate", WorkflowType.OpaqueType, description = "Dependency token used to serialize scene generation"),
    ),
    outputs = listOf(PortSpec("video", WorkflowType.OpaqueType, description = "Rendered scene video")),
)

private val ShortsBatchSubgraphNodeSpec = NodeSpec(
    type = SHORTS_BATCH_SUBGRAPH_NODE_TYPE,
    label = "Batch Stitch Subgraph",
    description = "Stitches a small batch of clips into one clip",
    category = SUBGRAPH_CATEGORY,
    inputs = listOf(
        PortSpec("video1", WorkflowType.OpaqueType),
        PortSpec("video2", WorkflowType.OpaqueType),
        PortSpec("video3", WorkflowType.OpaqueType),
        PortSpec("video4", WorkflowType.OpaqueType),
    ),
    outputs = listOf(PortSpec("video", WorkflowType.OpaqueType, description = "Stitched video")),
)

// GRAPHYN_SUBGRAPH_TYPE has no registered executor, so a bare subgraph node falls back to exposing
// every unconsumed output port from every internal node merged together — not a clean single result
// (confirmed via StoryboardDiagnosticTest.kt: a "graphyn.subgraph"-typed node exposed 5 unrelated keys
// from several internal nodes instead of just the terminal "validate" node's "result"). This type
// gets its own executor below that picks out specifically "result" — matching storyboardGeneratorSubgraph's
// terminal `validate` node's own output port name — same pattern as the scene/batch executors above.
private val StoryboardSubgraphNodeSpec = NodeSpec(
    type = STORYBOARD_SUBGRAPH_NODE_TYPE,
    label = "Storyboard Subgraph",
    description = "Runs the Ollama storyboard generator and exposes the validated result",
    category = SUBGRAPH_CATEGORY,
    inputs = emptyList(),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated storyboard record")),
)

private val StoryboardValidateNodeSpec = NodeSpec(
    type = STORYBOARD_VALIDATE_NODE_TYPE,
    label = "Storyboard Validate",
    description = "Validates the Ollama storyboard JSON and unloads the Ollama model (compiled, not scripted)",
    category = SUBGRAPH_CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("value", WorkflowType.OpaqueType, description = "Validated storyboard record")),
)

private val OllamaUnloadNodeSpec = NodeSpec(
    type = OLLAMA_UNLOAD_NODE_TYPE,
    label = "Ollama Unload",
    description = "Force-unloads the Ollama model; wire its 'gate' output into a scene subgraph to order it before generation",
    category = SUBGRAPH_CATEGORY,
    inputs = emptyList(),
    outputs = listOf(PortSpec("gate", WorkflowType.OpaqueType, description = "Dependency token — connect to a scene subgraph's 'gate' input")),
)

object SubgraphRuntimePlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "demo.subgraph",
        displayName = "Subgraph Demo",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(SubgraphNodeSpec)
        registrar.registerNodeSpec(ShortsSceneSubgraphNodeSpec)
        registrar.registerNodeSpec(ShortsBatchSubgraphNodeSpec)
        registrar.registerNodeSpec(StoryboardSubgraphNodeSpec)
        registrar.registerNodeSpec(StoryboardValidateNodeSpec)
        registrar.registerNodeSpec(OllamaUnloadNodeSpec)
        registrar.registerNodeSpec(ollamaUrlSpec)
        registrar.registerNodeSpec(ollamaBodySpec)
        registrar.registerNodeSpec(storyboardFieldSpec)
        registrar.registerNodeSpec(storyboardSceneFieldSpec)
        registrar.registerNodeSpec(storyboardCaptionsSpec)
        registrar.registerExecutor(SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("output" to (inputs["input"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(SHORTS_SCENE_SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("video" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(SHORTS_BATCH_SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("video" to (inputs["video"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(STORYBOARD_SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("value" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(STORYBOARD_VALIDATE_NODE_TYPE, storyboardValidateExecutor)
        registrar.registerExecutor(OLLAMA_UNLOAD_NODE_TYPE, ollamaUnloadExecutor)
        registrar.registerExecutor(OLLAMA_URL_NODE_TYPE, ollamaUrlExecutor)
        registrar.registerExecutor(OLLAMA_BODY_NODE_TYPE, ollamaBodyExecutor)
        registrar.registerExecutor(STORYBOARD_FIELD_NODE_TYPE, storyboardFieldExecutor)
        registrar.registerExecutor(STORYBOARD_SCENE_FIELD_NODE_TYPE, storyboardSceneFieldExecutor)
        registrar.registerExecutor(STORYBOARD_CAPTIONS_NODE_TYPE, storyboardCaptionsExecutor)
    }
}

object SubgraphEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "demo.subgraph.editor",
        displayName = "Subgraph Demo Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        registrar.registerCanvasCard(SUBGRAPH_NODE_TYPE, SubgraphCardFactory)
        registrar.registerCanvasCard(SHORTS_SCENE_SUBGRAPH_NODE_TYPE, SubgraphCardFactory)
        registrar.registerCanvasCard(SHORTS_BATCH_SUBGRAPH_NODE_TYPE, SubgraphCardFactory)
        registrar.registerCanvasCard(STORYBOARD_SUBGRAPH_NODE_TYPE, SubgraphCardFactory)
        registrar.registerCategory(SUBGRAPH_CATEGORY, NodeCategoryMeta("Composition", 0xFF7C3AED, group = NodeGroups.FLOW))
    }
}

private object SubgraphCardFactory : NodeCanvasFactory {
    override val nodeWidth = 280
    override val nodeHeight = 160

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = SubgraphCard(context)
}
