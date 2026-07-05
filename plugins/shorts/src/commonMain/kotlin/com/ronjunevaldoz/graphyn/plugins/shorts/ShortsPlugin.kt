package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * Runtime plugin for the storyboard-first shorts pipeline. Registers the Ollama storyboard
 * generator/validator executors, the storyboard field/scene/caption extractors, the Ollama-unload
 * gate, and the scene/batch/storyboard subgraph-wrapper nodes.
 *
 * The three subgraph-wrapper executors each pick out a single terminal output port of their nested
 * definition ("video" / "value") — a bare untyped subgraph node has no registered executor and
 * would instead fall back to merging every unconsumed internal output into one confusing record.
 *
 * ```kotlin
 * registry.install(ShortsPlugin)
 * val storyboard = storyboardGeneratorSubgraph("origami cranes")
 * ```
 */
public object ShortsPlugin : GraphynPlugin {
    override val metadata: GraphynPluginMetadata = GraphynPluginMetadata(
        id = "graphyn.shorts",
        displayName = "Shorts",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        registrar.registerNodeSpec(shortsSceneSubgraphSpec)
        registrar.registerNodeSpec(shortsBatchSubgraphSpec)
        registrar.registerNodeSpec(storyboardSubgraphSpec)
        registrar.registerNodeSpec(storyboardValidateSpec)
        registrar.registerNodeSpec(ollamaUnloadSpec)
        registrar.registerNodeSpec(ollamaUrlSpec)
        registrar.registerNodeSpec(ollamaBodySpec)
        registrar.registerNodeSpec(storyboardFieldSpec)
        registrar.registerNodeSpec(storyboardSceneFieldSpec)
        registrar.registerNodeSpec(storyboardCaptionsSpec)

        registrar.registerExecutor(ShortsNodeTypes.SCENE_SUBGRAPH) { inputs ->
            mapOf("video" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(ShortsNodeTypes.BATCH_SUBGRAPH) { inputs ->
            mapOf("video" to (inputs["video"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_SUBGRAPH) { inputs ->
            mapOf("value" to (inputs["value"] ?: WorkflowValue.NullValue))
        }
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_VALIDATE, storyboardValidateExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_UNLOAD, ollamaUnloadExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_URL, ollamaUrlExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_BODY, ollamaBodyExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_FIELD, storyboardFieldExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_SCENE_FIELD, storyboardSceneFieldExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_CAPTIONS, storyboardCaptionsExecutor)
    }
}
