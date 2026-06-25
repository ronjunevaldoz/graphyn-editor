package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.DataAndIo
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.Examples
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.Media

// script.eval is JVM-only; type string is used here so this KMP module stays dependency-free.
private val scriptDemoWorkflow = WorkflowDefinition(
    id = "script-demo", name = "Script",
    nodes = listOf(
        NodeRef("format", "text.format",
            config = mapOf("template" to WorkflowValue.StringValue("Hello, {name}!"))),
        NodeRef("script", "script.eval",
            config = mapOf("code" to WorkflowValue.StringValue(
                "import java.time.LocalDate\n" +
                "\"[\${LocalDate.now()}] \$input\""
            ))),
        NodeRef("preview", "preview.view"),
    ),
    connections = listOf(
        ConnectionRef("format",  "result", "script",  "input"),
        ConnectionRef("script",  "result", "preview", "value"),
    ),
)

/**
 * Catalog of bundled workflows surfaced in the launcher. Each entry carries its launcher
 * [category] and a one-line [description]; production templates use [Media]/[DataAndIo] and
 * node-type showcases use [Examples].
 */
enum class DemoScene(
    val label: String,
    val workflow: WorkflowDefinition,
    val category: WorkflowCategory,
    val description: String,
) {
    SimpleTts("Text to Speech", simpleTtsWorkflow, Media,
        "Read a text file and synthesize spoken audio."),
    VideoNarration("Video Narration", videoNarrationWorkflow, Media,
        "Mix a synthesized narration track into a video and re-encode."),
    AudioMix("Audio Mix", audioMixWorkflow, Media,
        "Blend a video's audio with a synthesized voice; style captions."),
    SmartEncode("Smart Encode", smartEncodeWorkflow, Media,
        "Pick an encode bitrate from the source duration via a script."),
    VideoStitch("Video Stitch", videoStitchWorkflow, Media,
        "Concatenate clips with hard cuts and export one MP4."),
    Captioned("Captioned Video", captionedVideoWorkflow, Media,
        "Transcribe a video's speech and burn the captions back in."),
    OcrExtract("Document Text Extract", documentOcrWorkflow, Media,
        "Read text and bounding blocks out of an image with OCR."),
    ApiIngestion("API Ingestion", apiIngestionDemoWorkflow, DataAndIo,
        "Fetch JSON over HTTP, extract fields, and persist to a file."),
    AiPipeline("AI Pipeline", aiPipelineWorkflow, Examples,
        "Showcase of the AI node category."),
    GeometryPipeline("Geometry", geometryPipelineWorkflow, Examples,
        "Showcase of the geometry node category."),
    AutomationPipeline("Automation", automationPipelineWorkflow, Examples,
        "Showcase of the automation node category."),
    Styles("Styles", styleNodesDemoWorkflow, Examples,
        "Showcase of the three sample card styles."),
    ListOps("List Ops", listOpsDemoWorkflow, Examples,
        "zip → map → filter → reduce list operations."),
    Control("Control", controlDemoWorkflow, Examples,
        "Loop, branch, and merge control-flow nodes."),
    Text("Text", textDemoWorkflow, Examples,
        "Format, split, and regex text operations."),
    Types("Types", typesDemoWorkflow, Examples,
        "Schema, cast, and validate type utilities."),
    Io("I/O", ioDemoWorkflow, Examples,
        "HTTP request, file read, and file write nodes."),
    Subgraph("Subgraph", subgraphDemoWorkflow, Examples,
        "A reusable pipeline nested inside a subgraph node."),
    Script("Script", scriptDemoWorkflow, Examples,
        "Run inline Kotlin Script over upstream values."),
}
