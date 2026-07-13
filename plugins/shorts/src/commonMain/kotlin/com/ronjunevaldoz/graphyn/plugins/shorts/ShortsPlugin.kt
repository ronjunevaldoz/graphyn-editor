package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * Runtime plugin for the storyboard-first shorts pipeline. Registers the Ollama storyboard
 * generator/validator executors, the storyboard field/scene/caption extractors, the Ollama-unload
 * gate, and the scene/batch/storyboard/ollama-fetch subgraph-wrapper nodes.
 *
 * Each subgraph-wrapper executor picks out its nested definition's named terminal output port(s)
 * ("video" / "value", or "value" + "diagnostics" for [ShortsNodeTypes.OLLAMA_FETCH_SUBGRAPH]) — a
 * bare untyped subgraph node has no registered executor and would instead fall back to merging every
 * unconsumed internal output into one confusing record.
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
        registrar.registerNodeSpec(ollamaFetchSubgraphSpec)
        registrar.registerNodeSpec(ollamaChainDiagnosticsSpec)
        registrar.registerNodeSpec(ollamaUrlSpec)
        registrar.registerNodeSpec(ollamaBodySpec)
        registrar.registerNodeSpec(storyboardFieldSpec)
        registrar.registerNodeSpec(storyboardSceneFieldSpec)
        registrar.registerNodeSpec(storyboardCaptionsSpec)
        registrar.registerNodeSpec(ollamaGenerateSpec)
        registrar.registerNodeSpec(comparisonOllamaBodySpec)
        registrar.registerNodeSpec(comparisonValidateSpec)
        registrar.registerNodeSpec(comparisonFieldsSpec)
        registrar.registerNodeSpec(comparisonPairFieldsSpec)
        registrar.registerNodeSpec(comparisonCaptionsSpec)
        registrar.registerNodeSpec(comparisonPairDurationSpec)
        registrar.registerNodeSpec(comparisonMetadataSpec)
        registrar.registerNodeSpec(promptEnhanceLlmSpec)

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
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_FETCH_SUBGRAPH) { inputs ->
            mapOf(
                "value" to (inputs["value"] ?: WorkflowValue.NullValue),
                "diagnostics" to (inputs["diagnostics"] ?: WorkflowValue.StringValue("no chain diagnostics wired")),
            )
        }
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_CHAIN_DIAGNOSTICS, ollamaChainDiagnosticsExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_URL, ollamaUrlExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_BODY, ollamaBodyExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_FIELD, storyboardFieldExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_SCENE_FIELD, storyboardSceneFieldExecutor)
        registrar.registerExecutor(ShortsNodeTypes.STORYBOARD_CAPTIONS, storyboardCaptionsExecutor)
        registrar.registerExecutor(ShortsNodeTypes.OLLAMA_GENERATE, ollamaGenerateExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_OLLAMA_BODY, comparisonOllamaBodyExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_VALIDATE, comparisonValidateExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_FIELDS, comparisonFieldsExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_PAIR_FIELDS, comparisonPairFieldsExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_CAPTIONS, comparisonCaptionsExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_PAIR_DURATION, comparisonPairDurationExecutor)
        registrar.registerExecutor(ShortsNodeTypes.COMPARISON_METADATA, comparisonMetadataExecutor)
        registrar.registerExecutor(ShortsNodeTypes.PROMPT_ENHANCE_LLM, promptEnhanceLlmExecutor)
    }
}
