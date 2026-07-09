package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val STORYBOARD_WORKFLOW_KEY = "storyboard"
internal const val RECAPTION_WORKFLOW_KEY = "recaption"
internal const val REGENERATE_SCENE_WORKFLOW_KEY = "regenerate-scene"
internal const val CHARACTER_SAMPLES_WORKFLOW_KEY = "character-samples"
internal const val COMPARISON_WORKFLOW_KEY = "comparison"
internal const val SCHEMA_MODE_KEY = "schema"

internal fun resolveTtsEngineChoice(options: Map<String, String>, default: TtsEngineChoice): TtsEngineChoice {
    val engine = options["tts_engine"] ?: return default
    val params = when (engine) {
        "say" -> mapOf("voice_id" to WorkflowValue.StringValue(options["voice_id"] ?: "Samantha"), "speed" to WorkflowValue.DoubleValue(options["speed"]?.toDoubleOrNull() ?: 1.0))
        "qwen3" -> mapOf("voice" to WorkflowValue.StringValue(options["voice"] ?: ""), "reference_audio_path" to WorkflowValue.StringValue(options["reference_audio_path"] ?: ""), "temperature" to WorkflowValue.DoubleValue(options["temperature"]?.toDoubleOrNull() ?: com.ronjunevaldoz.graphyn.plugins.mediaai.TtsEngineDefaults.QWEN3_TEMPERATURE))
        "oute" -> mapOf("language" to WorkflowValue.StringValue(options["language"] ?: "en"), "voice" to WorkflowValue.StringValue(options["voice"] ?: "default"), "instruct" to WorkflowValue.StringValue(options["instruct"] ?: ""), "temperature" to WorkflowValue.DoubleValue(options["temperature"]?.toDoubleOrNull() ?: com.ronjunevaldoz.graphyn.plugins.mediaai.TtsEngineDefaults.OUTE_TEMPERATURE), "seed" to WorkflowValue.IntValue(options["seed"]?.toIntOrNull() ?: com.ronjunevaldoz.graphyn.plugins.mediaai.TtsEngineDefaults.OUTE_SEED))
        else -> error("Unknown tts_engine '$engine'. Use say, qwen3, or oute.")
    }
    return TtsEngineChoice(engine, params)
}

internal fun applyNodeOverrides(workflow: WorkflowDefinition, options: Map<String, String>): WorkflowDefinition {
    val overridesByNode = options.entries.mapNotNull { (key, value) -> key.indexOf('.').takeIf { it > 0 }?.let { key.substring(0, it) to (key.substring(it + 1) to value) } }.groupBy({ it.first }, { it.second })
    if (overridesByNode.isEmpty()) return workflow
    return workflow.copy(nodes = workflow.nodes.map { node ->
        overridesByNode[node.id]?.let { ports ->
            node.copy(config = node.config + ports.associate { (port, value) ->
                val existing = node.config[port]
                port to (existing?.let { toWorkflowValueLike(it, value) } ?: WorkflowValue.StringValue(value))
            })
        } ?: node
    })
}

internal fun toWorkflowValueLike(default: WorkflowValue, raw: String): WorkflowValue = when (default) {
    is WorkflowValue.IntValue -> WorkflowValue.IntValue(raw.toInt())
    is WorkflowValue.DoubleValue -> WorkflowValue.DoubleValue(raw.toDouble())
    is WorkflowValue.ListValue -> WorkflowValue.ListValue(raw.split(',').map { WorkflowValue.StringValue(it.trim()) })
    is WorkflowValue.BooleanValue -> WorkflowValue.BooleanValue(raw.toBooleanStrict())
    else -> WorkflowValue.StringValue(raw)
}
