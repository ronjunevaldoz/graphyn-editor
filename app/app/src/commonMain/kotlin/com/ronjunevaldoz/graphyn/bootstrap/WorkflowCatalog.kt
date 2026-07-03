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
enum class WorkflowCatalog(
    val label: String,
    val workflow: WorkflowDefinition,
    val category: WorkflowCategory,
    val description: String,
    val badge: String? = null,
) {
    SimpleTts("Text to Speech", simpleTtsWorkflow, Media,
        "Read a text file and synthesize spoken audio.", badge = "Stable"),
    VideoNarration("Video Narration", videoNarrationWorkflow, Media,
        "Mix a synthesized narration track into a video and re-encode.", badge = "Stable"),
    AudioMix("Audio Mix", audioMixWorkflow, Media,
        "Blend a video's audio with a synthesized voice; style captions.", badge = "Stable"),
    SmartEncode("Smart Encode", smartEncodeWorkflow, Media,
        "Pick an encode bitrate from the source duration via a script.", badge = "Stable"),
    VideoStitch("Video Stitch", videoStitchWorkflow, Media,
        "Concatenate clips with hard cuts and export one MP4.", badge = "Stable"),
    Captioned("Captioned Video", captionedVideoWorkflow, Media,
        "Transcribe a video's speech and burn the captions back in.", badge = "Stable"),
    OcrExtract("Document Text Extract", documentOcrWorkflow, Media,
        "Read text and bounding blocks out of an image with OCR.", badge = "Stable"),
    PictureInPicture("Picture-in-Picture", pictureInPictureWorkflow, Media,
        "Overlay a second clip onto a base video for a timed window.", badge = "Stable"),
    SyncCalibration("Sync Calibration", syncCalibrationWorkflow, Media,
        "Average measured A/V drift into delay offsets.", badge = "Stable"),
    FluxTxt2Img("FLUX Text to Image", fluxTxt2ImgWorkflow, Media,
        "Generate an image from a prompt using FLUX.1-schnell (4-step distilled).", badge = "AI"),
    QwenTxt2Img("Qwen Image Text to Image", qwenTxt2ImgWorkflow, Media,
        "Generate an image with Qwen-Image (2512) + 4-step Lightning LoRA.", badge = "AI"),
    QwenImg2Img("Qwen Image Edit", qwenImg2ImgWorkflow, Media,
        "Edit an image with Qwen-Image-Edit (2511) + 4-step Lightning LoRA.", badge = "AI"),
    WanImg2Vid("Wan Image to Video (720p)", wanImg2VidWorkflow, Media,
        "Animate a still into video with Wan2.2 A14B (MoE) — 720P quality tier, needs >12GB VRAM.", badge = "AI"),
    Wan480pImg2Vid("Wan Image to Video (480p)", wan480pImg2VidWorkflow, Media,
        "Animate a still into video with Wan2.2 A14B (MoE) — 480P quality tier, same model family.", badge = "AI"),
    Wan5bImg2Vid("Wan Image to Video (5B)", wan5bImg2VidWorkflow, Media,
        "Animate a still with Wan2.2-TI2V-5B — fits a 12GB card. Fast tier.", badge = "AI"),
    ImageShorts("AI Shorts (Image Motion)", imageShortsWorkflow, Media,
        "Generate FLUX keyframes, repeat them into scene clips, stitch, caption, and encode.", badge = "AI"),
    VideoShorts("AI Shorts (Video Motion)", videoShortsWorkflow, Media,
        "Generate motion-first scene clips with Wan, then stitch, caption, and encode.", badge = "AI"),
    ImageEdit("Image Edit", imageEditWorkflow, Media,
        "Resize an image and crop a region out of it.", badge = "Stable"),
    Slideshow("Slideshow", slideshowWorkflow, Media,
        "Render a set of images into a video at a fixed frame rate.", badge = "Stable"),
    ApiIngestion("API Ingestion", apiIngestionDemoWorkflow, DataAndIo,
        "Fetch JSON over HTTP, extract fields, and persist to a file.", badge = "Stable"),
    AiPipeline("AI Pipeline", aiPipelineWorkflow, Examples,
        "Showcase of the AI node category.", badge = "Demo"),
    GeometryPipeline("Geometry", geometryPipelineWorkflow, Examples,
        "Showcase of the geometry node category.", badge = "Demo"),
    AutomationPipeline("Automation", automationPipelineWorkflow, Examples,
        "Showcase of the automation node category.", badge = "Demo"),
    Styles("Styles", styleNodesDemoWorkflow, Examples,
        "Showcase of the three sample card styles.", badge = "Demo"),
    ListOps("List Ops", listOpsDemoWorkflow, Examples,
        "zip → map → filter → reduce list operations.", badge = "Demo"),
    Control("Control", controlDemoWorkflow, Examples,
        "Loop, branch, and merge control-flow nodes.", badge = "Demo"),
    Text("Text", textDemoWorkflow, Examples,
        "Format, split, and regex text operations.", badge = "Demo"),
    Types("Types", typesDemoWorkflow, Examples,
        "Schema, cast, and validate type utilities.", badge = "Demo"),
    Io("I/O", ioDemoWorkflow, Examples,
        "HTTP request, file read, and file write nodes.", badge = "Demo"),
    Subgraph("Subgraph", subgraphDemoWorkflow, Examples,
        "A reusable pipeline nested inside a subgraph node.", badge = "Demo"),
    Script("Script", scriptDemoWorkflow, Examples,
        "Run inline Kotlin Script over upstream values.", badge = "Demo"),
}
