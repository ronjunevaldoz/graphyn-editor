package com.ronjunevaldoz.graphyn.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasContext
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCanvasFactory
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

const val SUBGRAPH_NODE_TYPE = "demo.subgraph"
private const val SUBGRAPH_CATEGORY = "demo.composition"
internal const val SUBGRAPH_LABEL_KEY = "label"
internal const val SUBGRAPH_COUNT_KEY = "node_count"

val SubgraphNodeSpec = NodeSpec(
    type = SUBGRAPH_NODE_TYPE,
    label = "Subgraph",
    description = "Encapsulates a nested workflow as a single node",
    category = SUBGRAPH_CATEGORY,
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, description = "Data into the subgraph")),
    outputs = listOf(PortSpec("output", WorkflowType.OpaqueType, description = "Data produced by the subgraph")),
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
        registrar.registerExecutor(SUBGRAPH_NODE_TYPE) { inputs ->
            mapOf("output" to (inputs["input"] ?: WorkflowValue.NullValue))
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
        registrar.registerCategory(SUBGRAPH_CATEGORY, NodeCategoryMeta("Composition", 0xFF7C3AED))
    }
}

private object SubgraphCardFactory : NodeCanvasFactory {
    override val nodeWidth = 280
    override val nodeHeight = 160

    @Composable
    override fun NodeCanvas(context: NodeCanvasContext) = SubgraphCard(context)
}

private val SHAPE = RoundedCornerShape(8.dp)
private val BG = Color(0xFF1E1B3A)
private val HEADER_BG = Color(0xFF7C3AED)
private val BORDER_DEFAULT = Color(0xFF5B21B6)
private val BORDER_SELECTED = Color(0xFFA78BFA)
private val TEXT_LIGHT = Color(0xFFEDE9FE)
private val TEXT_MUTED = Color(0xFFA78BFA)

@Composable
private fun SubgraphCard(ctx: NodeCanvasContext) {
    val label = (ctx.node.config[SUBGRAPH_LABEL_KEY] as? WorkflowValue.StringValue)?.value
        ?: ctx.spec.label
    val count = (ctx.node.config[SUBGRAPH_COUNT_KEY] as? WorkflowValue.IntValue)?.value ?: 0
    val border = if (ctx.selected) BORDER_SELECTED else BORDER_DEFAULT

    Column(
        Modifier
            .size(280.dp, 160.dp)
            .clip(SHAPE)
            .background(BG)
            .border(1.5.dp, border, SHAPE)
            .clickable { ctx.onSelect() },
    ) {
        Row(
            Modifier.fillMaxWidth().height(36.dp).background(HEADER_BG).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("⊞", style = TextStyle(color = TEXT_LIGHT, fontSize = 14.sp))
            Spacer(Modifier.width(6.dp))
            BasicText(label, style = TextStyle(color = TEXT_LIGHT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
            BasicText(
                "Nested workflow",
                style = TextStyle(color = TEXT_MUTED, fontSize = 10.sp),
            )
            if (count > 0) {
                Box(
                    Modifier.align(Alignment.BottomEnd).clip(CircleShape)
                        .background(HEADER_BG).padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    BasicText(
                        "$count nodes",
                        style = TextStyle(color = TEXT_LIGHT, fontSize = 9.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}
