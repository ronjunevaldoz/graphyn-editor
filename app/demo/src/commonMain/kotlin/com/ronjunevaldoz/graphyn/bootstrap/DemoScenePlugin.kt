package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.canvas.NodeCategoryMeta
import com.ronjunevaldoz.graphyn.editor.plugins.GRAPHYN_EDITOR_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPlugin
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginMetadata
import com.ronjunevaldoz.graphyn.editor.plugins.GraphynEditorPluginRegistrar
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_AI
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_AUTOMATION
import com.ronjunevaldoz.graphyn.plugins.stylenodes.CATEGORY_GEOMETRY
import com.ronjunevaldoz.graphyn.ui.cards.FieldCardFactory
import com.ronjunevaldoz.graphyn.ui.cards.ShapeCardFactory

private val allSpecs = listOf(
    specCheckpointLoader, specClipEncode, specVaeDecode, specSaveImage,
    specMeshPrimitive, specSubdivideMesh, specInstanceOnPoints, specGeometryOutput,
    specSetField, specFilterIf, specHttpRequestDemo, specLogOutput,
)

object DemoSceneRuntimePlugin : GraphynPlugin {
    override val metadata = GraphynPluginMetadata(
        id = "demo.scenes",
        displayName = "Demo Scene Nodes",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        allSpecs.forEach { registrar.registerNodeSpec(it) }
        // Passthrough executors — forward first output port value, else NullValue
        registrar.registerExecutor(specCheckpointLoader.type) { _ ->
            mapOf("model" to WorkflowValue.NullValue, "clip" to WorkflowValue.NullValue, "vae" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specClipEncode.type) { _ ->
            mapOf("conditioning" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specVaeDecode.type) { inputs ->
            mapOf("image" to (inputs["samples"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(specSaveImage.type) { _ -> emptyMap() }
        registrar.registerExecutor(specMeshPrimitive.type) { _ ->
            mapOf("geometry" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specSubdivideMesh.type) { inputs ->
            mapOf("geometry" to (inputs["geometry"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(specInstanceOnPoints.type) { _ ->
            mapOf("geometry" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specGeometryOutput.type) { _ -> emptyMap() }
        registrar.registerExecutor(specSetField.type) { inputs ->
            mapOf("record" to (inputs["record"] ?: WorkflowValue.RecordValue(emptyMap())))
        }
        registrar.registerExecutor(specFilterIf.type) { inputs ->
            mapOf("value" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(specHttpRequestDemo.type) { _ ->
            mapOf("response" to WorkflowValue.NullValue)
        }
        registrar.registerExecutor(specLogOutput.type) { _ -> emptyMap() }
    }
}

object DemoSceneEditorPlugin : GraphynEditorPlugin {
    override val metadata = GraphynEditorPluginMetadata(
        id = "demo.scenes.editor",
        displayName = "Demo Scene Editor",
        version = "1.0.0",
        apiVersion = GRAPHYN_EDITOR_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynEditorPluginRegistrar) {
        val shape = ShapeCardFactory()
        val field3 = FieldCardFactory(inputRows = 3, outputRows = 1)
        // CheckpointLoader has no inputs; CLIPTextEncode has 2 (clip + text); show text inline
        registrar.registerCanvasCard(specCheckpointLoader.type, shape)
        registrar.registerCanvasCard(specClipEncode.type, ShapeCardFactory(inlineInputRows = 1))
        registrar.registerCanvasCard(specVaeDecode.type, shape)
        registrar.registerCanvasCard(specSaveImage.type, shape)
        listOf(specMeshPrimitive, specSubdivideMesh, specInstanceOnPoints, specGeometryOutput).forEach {
            registrar.registerCanvasCard(it.type, field3)
        }
        // HTTPRequest has url + method defaults; show them inline
        registrar.registerCanvasCard(specSetField.type, shape)
        registrar.registerCanvasCard(specFilterIf.type, shape)
        registrar.registerCanvasCard(specHttpRequestDemo.type, ShapeCardFactory(inlineInputRows = 2))
        registrar.registerCanvasCard(specLogOutput.type, shape)
        registrar.registerCategory(CATEGORY_AI,         NodeCategoryMeta("AI",         PORT_MODEL))
        registrar.registerCategory(CATEGORY_GEOMETRY,   NodeCategoryMeta("Geometry",   0xFF3DC95AL))
        registrar.registerCategory(CATEGORY_AUTOMATION, NodeCategoryMeta("Automation", 0xFFFF9900L))
    }
}
