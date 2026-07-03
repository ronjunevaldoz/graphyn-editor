package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

internal const val SHORTS_SCENE_MS = 4_000
internal const val SHORTS_SCENE_COUNT = 8
internal const val SHORTS_FPS = 16
internal const val SHORTS_FRAME_COUNT = 64
internal const val SHORTS_WIDTH = 720
internal const val SHORTS_HEIGHT = 1280

internal val SHORTS_BODY_SCRIPT = """
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
val model = (input as? String)?.ifBlank { null } ?: "llama3.1"
val prompt = listOf(
    "Write JSON only for a vertical short plan.",
    "Invent a specific topic, tone, and visual language instead of staying generic.",
    "Return this shape:",
    "{",
    "  \"title\": string,",
    "  \"topic\": string,",
    "  \"visual_style\": string,",
    "  \"scenes\": [",
    "    {",
    "      \"prompt\": string,",
    "      \"caption\": string,",
    "      \"camera_move\": string",
    "    }",
    "  ]",
    "}",
    "Make exactly 8 scenes.",
    "Each scene is 4 seconds.",
    "Keep the story concrete, visual, and scene-driven.",
).joinToString("\n")
WorkflowValue.RecordValue(
    mapOf(
        "model" to WorkflowValue.StringValue(model),
        "prompt" to WorkflowValue.StringValue(prompt),
        "stream" to WorkflowValue.BooleanValue(false),
        "format" to WorkflowValue.StringValue("json"),
    ),
)
""".trimIndent()

internal val SHORTS_CAPTIONS_SCRIPT = """
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
val scenes = input as List<*>
WorkflowValue.ListValue(
    scenes.mapIndexed { index, scene ->
        val fields = scene as Map<*, *>
        WorkflowValue.RecordValue(
            mapOf(
                "text" to WorkflowValue.StringValue((fields["caption"] as? String).orEmpty()),
                "start_ms" to WorkflowValue.DoubleValue(index * 4000.0),
                "end_ms" to WorkflowValue.DoubleValue((index + 1) * 4000.0),
            ),
        )
    },
)
""".trimIndent()

internal val SHORTS_WRAP_FRAMES_SCRIPT = """
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
val paths = input as List<*>
WorkflowValue.ListValue(paths.filterIsInstance<String>().map { MediaTypes.imageValue(it) })
""".trimIndent()

internal val SHORTS_WRAP_KEYFRAME_SCRIPT = """
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediacore.MediaTypes
val image = when (input) {
    is Map<*, *> -> input["path"] as? String
    is String -> input
    else -> null
}.orEmpty()
WorkflowValue.ListValue(List($SHORTS_FRAME_COUNT) { MediaTypes.imageValue(image) })
""".trimIndent()

internal fun shortsScenePromptScript(index: Int) = """
val scenes = input as? List<*> ?: emptyList<Any?>()
val scene = scenes.getOrNull(${index - 1}) as? Map<*, *> ?: emptyMap<String, Any?>()
listOf(
    scene["prompt"] as? String,
    scene["caption"] as? String,
    scene["title"] as? String,
    scene["topic"] as? String,
    scene["visual_style"] as? String,
    scene["camera_move"] as? String,
    scene["framing"] as? String,
    scene["lighting"] as? String,
    scene["details"] as? String,
).firstOrNull { !it.isNullOrBlank() }.orEmpty()
""".trimIndent()

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)

internal fun shortsCaptionStyleNode() = NodeRef(
    "captionStyle", CAPTION_STYLE_NODE_TYPE,
    config = CAPTION_STYLE_DEFAULTS,
)
