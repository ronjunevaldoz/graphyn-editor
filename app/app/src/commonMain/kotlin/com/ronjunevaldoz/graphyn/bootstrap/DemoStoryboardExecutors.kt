package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.execution.NodeExecutor
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.stringOr

// Compiled executors for the storyboard pipeline — see DemoStoryboardValidate.kt's doc comment for
// why: script.eval's shared JSR-223 engine corrupts its own compiler state after a few sequential
// different scripts, crashing later ones even when trivially simple. These replace every
// storyboard-related script.eval node with real, type-checked Kotlin instead.

internal const val OLLAMA_URL_NODE_TYPE = "demo.storyboard.ollama_url"
internal const val OLLAMA_BODY_NODE_TYPE = "demo.storyboard.ollama_body"
internal const val STORYBOARD_FIELD_NODE_TYPE = "demo.storyboard.field"
internal const val STORYBOARD_SCENE_FIELD_NODE_TYPE = "demo.storyboard.scene_field"
internal const val STORYBOARD_CAPTIONS_NODE_TYPE = "demo.storyboard.captions"

internal val ollamaUrlSpec = NodeSpec(
    type = OLLAMA_URL_NODE_TYPE, label = "Ollama URL", category = SUBGRAPH_CATEGORY,
    description = "Builds the /api/generate URL from an Ollama host string.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false)),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)
internal val ollamaUrlExecutor = NodeExecutor { inputs ->
    val host = ((inputs["input"] as? WorkflowValue.StringValue)?.value?.ifBlank { null } ?: "http://localhost:11434")
        .let { if (it.endsWith("/")) it.dropLast(1) else it }
    mapOf("result" to WorkflowValue.StringValue("$host/api/generate"))
}

internal val ollamaBodySpec = NodeSpec(
    type = OLLAMA_BODY_NODE_TYPE, label = "Ollama Body", category = SUBGRAPH_CATEGORY,
    description = "Builds the Ollama /api/generate request body for the storyboard prompt.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("topic", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)
internal val ollamaBodyExecutor = NodeExecutor { inputs ->
    val model = (inputs["input"] as? WorkflowValue.StringValue)?.value?.ifBlank { null } ?: "llama3.1"
    val topic = inputs.stringOr("topic", "")
    mapOf("result" to WorkflowValue.RecordValue(
        mapOf(
            "model" to WorkflowValue.StringValue(model),
            "prompt" to WorkflowValue.StringValue(buildStoryboardPrompt(topic)),
            "stream" to WorkflowValue.BooleanValue(false),
            "format" to WorkflowValue.StringValue("json"),
            // Belt-and-suspenders with unloadOllamaModel()'s follow-up call: that call is wrapped in
            // runCatching and silently swallows failures, so if it ever doesn't fire, this makes
            // Ollama drop the model itself right after answering instead of holding it for the
            // default 5-minute keep-alive while server-sd's Flux scenes run on the same GPU.
            "keep_alive" to WorkflowValue.IntValue(0),
        ),
    ))
}

internal val storyboardFieldSpec = NodeSpec(
    type = STORYBOARD_FIELD_NODE_TYPE, label = "Storyboard Field", category = SUBGRAPH_CATEGORY,
    description = "Extracts a single string field from a validated storyboard record.",
    inputs = listOf(PortSpec("input", WorkflowType.OpaqueType, required = false), PortSpec("field", WorkflowType.StringType)),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)
internal val storyboardFieldExecutor = NodeExecutor { inputs ->
    val field = inputs.stringOr("field", "")
    val record = (inputs["input"] as? WorkflowValue.RecordValue)?.fields
    val value = (record?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

internal val storyboardSceneFieldSpec = NodeSpec(
    type = STORYBOARD_SCENE_FIELD_NODE_TYPE, label = "Storyboard Scene Field", category = SUBGRAPH_CATEGORY,
    description = "Extracts one field of one scene (by index) from a validated storyboard record.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("index", WorkflowType.IntType),
        PortSpec("field", WorkflowType.StringType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.StringType)),
)
internal val storyboardSceneFieldExecutor = NodeExecutor { inputs ->
    val index = (inputs["index"] as? WorkflowValue.IntValue)?.value ?: 0
    val field = inputs.stringOr("field", "")
    val scenes = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("scenes") as? WorkflowValue.ListValue
    val scene = scenes?.items?.getOrNull(index) as? WorkflowValue.RecordValue
    val value = (scene?.fields?.get(field) as? WorkflowValue.StringValue)?.value.orEmpty()
    mapOf("result" to WorkflowValue.StringValue(value))
}

internal val storyboardCaptionsSpec = NodeSpec(
    type = STORYBOARD_CAPTIONS_NODE_TYPE, label = "Storyboard Captions", category = SUBGRAPH_CATEGORY,
    description = "Builds caption records (text/start_ms/end_ms) from a validated storyboard's scenes.",
    inputs = listOf(
        PortSpec("input", WorkflowType.OpaqueType, required = false),
        PortSpec("scene_duration_ms", WorkflowType.DoubleType),
    ),
    outputs = listOf(PortSpec("result", WorkflowType.OpaqueType)),
)
internal val storyboardCaptionsExecutor = NodeExecutor { inputs ->
    val sceneDurationMs = (inputs["scene_duration_ms"] as? WorkflowValue.DoubleValue)?.value ?: 2000.0
    val scenes = (inputs["input"] as? WorkflowValue.RecordValue)?.fields?.get("scenes") as? WorkflowValue.ListValue
    val captions = scenes?.items?.mapIndexed { index, scene ->
        val caption = ((scene as? WorkflowValue.RecordValue)?.fields?.get("caption") as? WorkflowValue.StringValue)?.value.orEmpty()
        WorkflowValue.RecordValue(
            mapOf(
                "text" to WorkflowValue.StringValue(caption),
                "start_ms" to WorkflowValue.DoubleValue(index * sceneDurationMs),
                "end_ms" to WorkflowValue.DoubleValue((index + 1) * sceneDurationMs),
            ),
        )
    }.orEmpty()
    mapOf("result" to WorkflowValue.ListValue(captions))
}

private fun buildStoryboardPrompt(topic: String) = listOf(
    "Write JSON only for a short-form vertical video storyboard about: $topic.",
    "Return exactly this shape:",
    "{",
    "  \"niche\": string,",
    "  \"visual_style\": string,",
    "  \"character\": string,",
    "  \"narration\": string,",
    "  \"scenes\": [ { \"prompt\": string, \"caption\": string } ]",
    "}",
    "Make exactly $STORYBOARD_SCENE_COUNT scenes.",
    "narration is the full spoken voiceover for the whole video, written as one flowing script.",
    "Each scene's caption is a short on-screen text (under 8 words), and prompt is a concrete visual",
    "description for a text-to-image model — specific subject, setting, lighting, no camera jargon.",
    "visual_style is a rendering style that applies to every scene — pick whichever best fits the",
    "topic instead of defaulting to photography, e.g. \"2D anime cel-shaded\", \"Pixar-style 3D cartoon\",",
    "\"flat vector illustration\", \"claymation stop-motion\", \"watercolor painting\", \"pixel art\", or",
    "\"warm cinematic photography, shallow depth of field\" for realistic subjects.",
    "character is a short, reusable visual description of the main subject (appearance, clothing,",
    "distinguishing features — no name needed) so it can be repeated identically in every scene's",
    "prompt for visual continuity. Leave it \"\" if no recurring subject fits the topic.",
).joinToString("\n")
