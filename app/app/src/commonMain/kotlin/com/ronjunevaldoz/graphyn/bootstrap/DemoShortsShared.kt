package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.plugins.mediaai.MediaAiSpecs

private const val OLLAMA_MODEL_ENV = "GRAPHYN_OLLAMA_MODEL"
private const val OLLAMA_HOST_ENV = "GRAPHYN_OLLAMA_HOST"
internal const val SHORTS_SCENE_MS = 4_000
internal const val SHORTS_SCENE_COUNT = 8
internal const val SHORTS_FPS = 16
internal const val SHORTS_FRAME_COUNT = 64
internal const val SHORTS_WIDTH = 720
internal const val SHORTS_HEIGHT = 1280

internal val SHORTS_BODY_SCRIPT = """
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
val model = (input as? String)?.ifBlank { null } ?: "llama3.1"
val prompt = "Write JSON only for an 8-scene vertical short. " +
    "Return {\"title\":string,\"scenes\":[{\"prompt\":string,\"caption\":string} x8]}. " +
    "Each scene is 4 seconds, total 32 seconds, and every prompt should include a camera move."
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

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)

internal fun shortsCaptionStyleNode() = NodeRef(
    "captionStyle", MediaAiSpecs.captionStyle.type,
    config = MediaAiSpecs.captionStyle.defaultValues,
)

internal fun shortsOutlineNodes() = listOf(
    NodeRef("ollama_host", "env.read", config = mapOf("name" to s(OLLAMA_HOST_ENV))),
    NodeRef("ollama_model", "env.read", config = mapOf("name" to s(OLLAMA_MODEL_ENV))),
    NodeRef("ollama_url", "script.eval", config = mapOf("code" to s(
        """val host = (input as? String)?.ifBlank { null } ?: "http://localhost:11434"
if (host.endsWith("/")) host.dropLast(1) + "/api/generate" else host + "/api/generate"""
    ))),
    NodeRef("ollama_body", "script.eval", config = mapOf("code" to s(SHORTS_BODY_SCRIPT))),
    NodeRef("body_json", "json.stringify"),
    NodeRef("request", "io.http_request", config = mapOf(
        "method" to s("POST"),
        "headers" to WorkflowValue.RecordValue(mapOf("Content-Type" to s("application/json"))),
    )),
    NodeRef("outer", "json.parse"),
    NodeRef("response", "json.path", config = mapOf("path" to s("response"))),
    NodeRef("outline", "json.parse"),
    NodeRef("scenes", "json.path", config = mapOf("path" to s("scenes"))),
    NodeRef("captions", "script.eval", config = mapOf("code" to s(SHORTS_CAPTIONS_SCRIPT))),
)
