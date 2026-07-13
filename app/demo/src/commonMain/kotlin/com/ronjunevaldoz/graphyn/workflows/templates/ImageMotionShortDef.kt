package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

private val IMAGE_MOTION_SCENES = listOf(
    Triple(
        "a chef plating a dish in a modern kitchen",
        "cooking",
        "This is how a five-star dish comes together.",
    ),
    Triple(
        "steam rising from a fresh bowl of ramen, close-up",
        "cooking",
        "Fresh ramen, made from scratch.",
    ),
    Triple(
        "a plated dish served at a restaurant table, warm lighting",
        "cooking",
        "And that's how it's served.",
    ),
)

// Splits the stitched clip's real duration evenly across the known captions — plain arithmetic on a
// numeric duration_ms, not a parse of externally-sourced JSON, so it can't hit the shape mismatch
// that broke captions in the Ollama-driven pipeline (imageShortsWorkflow).
private fun captionsScript(captions: List<String>): String {
    val literalCaptions = captions.joinToString(",\n") { "\"${it.replace("\"", "\\\"")}\"" }
    return """
    import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
    val totalMs = (input as? Double) ?: (input as? Int)?.toDouble() ?: 0.0
    val captions = listOf($literalCaptions)
    val perScene = if (captions.isNotEmpty()) totalMs / captions.size else 0.0
    WorkflowValue.ListValue(
        captions.mapIndexed { index, text ->
            WorkflowValue.RecordValue(
                mapOf(
                    "text" to WorkflowValue.StringValue(text),
                    "start_ms" to WorkflowValue.DoubleValue(index * perScene),
                    "end_ms" to WorkflowValue.DoubleValue((index + 1) * perScene),
                ),
            )
        },
    )
    """.trimIndent()
}

/**
 * Simple image-motion short: Flux scenes (niche-aware prompts, no Ollama outline step) stitched into
 * one clip, with on-screen captions and a synthesized narration track. Deliberately smaller than
 * [imageShortsWorkflow] — that one drives 8 scenes from an LLM-generated outline; this one is a plain,
 * reusable building block. Duplicate the `imageMotionSceneSubgraph(...)` pattern for more scenes.
 */
internal val imageMotionShortWorkflow = WorkflowDefinition(
    id = "image-motion-short",
    name = "Image Motion Short (Simple)",
    nodes = buildList {
        add(guideNote(
            """
            Image Motion Short · simple

            Flux scenes stitched into one clip, with captions + narration.
            Edit IMAGE_MOTION_SCENES in DemoImageMotionShortDef.kt to change
            the story, or duplicate imageMotionSceneSubgraph(...) for more scenes.
            """,
        ))
        IMAGE_MOTION_SCENES.forEachIndexed { index, (prompt, niche, _) ->
            add(NodeRef(
                "scene${index + 1}", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = imageMotionSceneSubgraph(prompt = prompt, niche = niche),
            ))
        }
        add(NodeRef("stitch", SHORTS_BATCH_SUBGRAPH_NODE_TYPE, subgraph = stitchBatchSubgraph(0)))
        add(NodeRef("captionsScript", "script.eval", config = mapOf(
            "code" to s(captionsScript(IMAGE_MOTION_SCENES.map { it.third })),
        )))
        add(shortsCaptionStyleNode())
        add(NodeRef("captionOverlay", "media.caption_overlay"))
        add(NodeRef("narrate", "media.text_to_speech", config = mapOf(
            "text" to s(IMAGE_MOTION_SCENES.joinToString(" ") { it.third }),
            "language" to s("en"), "voice_id" to s("Samantha"), "speed" to d(1.0),
        )))
        add(NodeRef("encode", "media.video_encode", config = mapOf(
            "output_path" to s("image-motion-short.mp4"), "bitrate" to s("high"), "codec" to s("h264"),
        )))
        add(NodeRef("output", "media.file_output"))
    },
    connections = buildList {
        IMAGE_MOTION_SCENES.forEachIndexed { index, _ ->
            add(ConnectionRef("scene${index + 1}", "video", "stitch", "video${index + 1}"))
        }
        add(ConnectionRef("stitch", "duration_ms", "captionsScript", "input"))
        add(ConnectionRef("stitch", "video", "captionOverlay", "video"))
        add(ConnectionRef("captionsScript", "result", "captionOverlay", "captions"))
        add(ConnectionRef("captionStyle", "style_config", "captionOverlay", "style_config"))
        add(ConnectionRef("captionOverlay", "video", "encode", "video"))
        add(ConnectionRef("narrate", "audio", "encode", "audio"))
        add(ConnectionRef("encode", "file_path", "output", "file_path"))
    },
)
