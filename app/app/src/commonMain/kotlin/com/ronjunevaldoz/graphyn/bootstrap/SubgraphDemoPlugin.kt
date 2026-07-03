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
private const val SUBGRAPH_CATEGORY = "demo.composition"

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
        PortSpec("prompt", WorkflowType.OpaqueType, description = "Scene prompt"),
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
        registrar.registerExecutor(SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("output" to (inputs["input"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(SHORTS_SCENE_SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("video" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(SHORTS_BATCH_SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("video" to (inputs["video"] ?: WorkflowValue.NullValue))
        }
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
        registrar.registerCategory(SUBGRAPH_CATEGORY, NodeCategoryMeta("Composition", 0xFF7C3AED, group = NodeGroups.FLOW))
    }
}

private object SubgraphCardFactory : NodeCanvasFactory {
    override val nodeWidth = 280
    override val nodeHeight = 160

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = SubgraphCard(context)
}
