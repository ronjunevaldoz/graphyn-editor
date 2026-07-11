package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.DataAndIo
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.Examples
import com.ronjunevaldoz.graphyn.editor.launcher.WorkflowCategory.Media
import com.ronjunevaldoz.graphyn.bootstrap.aiPipelineWorkflow
import com.ronjunevaldoz.graphyn.bootstrap.automationPipelineWorkflow
import com.ronjunevaldoz.graphyn.bootstrap.geometryPipelineWorkflow

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
    val badges: List<String> = emptyList(),
) {
    SimpleTts("Text to Speech", simpleTtsWorkflow, Media,
        "Read a text file and synthesize spoken audio.", badges = listOf("Stable")),
    VideoNarration("Video Narration", videoNarrationWorkflow, Media,
        "Mix a synthesized narration track into a video and re-encode.", badges = listOf("Stable")),
    AudioMix("Audio Mix", audioMixWorkflow, Media,
        "Blend a video's audio with a synthesized voice; style captions.", badges = listOf("Stable")),
    SmartEncode("Smart Encode", smartEncodeWorkflow, Media,
        "Pick an encode bitrate from the source duration via a script.", badges = listOf("Stable")),
    VideoStitch("Video Stitch", videoStitchWorkflow, Media,
        "Concatenate clips with hard cuts and export one MP4.", badges = listOf("Stable")),
    Captioned("Captioned Video", captionedVideoWorkflow, Media,
        "Transcribe a video's speech and burn the captions back in.", badges = listOf("Stable")),
    OcrExtract("Document Text Extract", documentOcrWorkflow, Media,
        "Read text and bounding blocks out of an image with OCR.", badges = listOf("Stable")),
    PictureInPicture("Picture-in-Picture", pictureInPictureWorkflow, Media,
        "Overlay a second clip onto a base video for a timed window.", badges = listOf("Stable")),
    SyncCalibration("Sync Calibration", syncCalibrationWorkflow, Media,
        "Average measured A/V drift into delay offsets.", badges = listOf("Stable")),
    FluxTxt2Img("FLUX Text to Image", fluxTxt2ImgWorkflow, Media,
        "Generate an image from a prompt using FLUX.1-schnell (4-step distilled).", badges = listOf("AI", "Stable")),
    QwenTxt2Img("Qwen Image Text to Image", qwenTxt2ImgWorkflow, Media,
        "Generate an image with Qwen-Image (2512) + 4-step Lightning LoRA.", badges = listOf("AI")),
    QwenImg2Img("Qwen Image Edit", qwenImg2ImgWorkflow, Media,
        "Edit an image with Qwen-Image-Edit (2511) + 4-step Lightning LoRA.", badges = listOf("AI")),
    FluxKontextImg2Img("FLUX Kontext Image Edit", fluxKontextImg2ImgWorkflow, Media,
        "Edit an image from an instruction prompt with FLUX.1 Kontext-dev — 4-6GB VRAM, no offload.", badges = listOf("AI")),
    ReferenceImageEdit(
        label = "Reference Image Edit (ad hoc)",
        // Click "Browse" on the file_browse node to pick a reference image visually, edit the
        // generate node's prompt directly in the graph, or drive the same builder from the CLI:
        // workflow=image-edit image=<path> instruction=<text> model=flux-kontext|qwen
        workflow = referenceImageEditEditorWorkflow(
            instruction = "describe the edit you want here",
            model = "flux-kontext",
        ),
        category = Media,
        description = "Edit any reference image from a runtime-supplied instruction — click Browse to pick your own image, edit the prompt directly in this node graph.",
        badges = listOf("AI"),
    ),
    PhotoMakerSdxl("SDXL PhotoMaker v2 (Identity)", photoMakerSdxlWorkflow, Media,
        "Identity-preserving generation from a character's precomputed id_embeds.bin.", badges = listOf("AI")),
    WanImg2Vid("Wan Image to Video (720p)", wanImg2VidWorkflow, Media,
        "Animate a still into video with Wan2.2 A14B (MoE) — 720P quality tier, needs >12GB VRAM.", badges = listOf("AI", "Stable")),
    Wan480pImg2Vid("Wan Image to Video (480p)", wan480pImg2VidWorkflow, Media,
        "Animate a still into video with Wan2.2 A14B (MoE) — 480P quality tier, same model family.", badges = listOf("AI", "Stable")),
    Wan5bImg2Vid("Wan Image to Video (5B)", wan5bImg2VidWorkflow, Media,
        "Animate a still with Wan2.2-TI2V-5B — fits a 12GB card. Fast tier.", badges = listOf("AI")),
    Wan14b480pImg2Vid("Wan Image to Video (14B 480p, single-model)", wan14b480pImg2VidWorkflow, Media,
        "Animate a still with Wan2.1-I2V-14B-480P + 4-step lightx2v LoRA — single-model, no MoE pass.", badges = listOf("AI")),
    ImageComparisonShort(
        label = "AI Image Comparison (Image Motion)",
        // mascotDescription left at its default (T-pose, see MascotScene.kt's
        // DEFAULT_MASCOT_DESCRIPTION) instead of the stale hardcoded "Stickman" override this
        // entry previously had — that override predated the mascot's Kontext-edit redesign and no
        // longer reflects what the pipeline actually generates.
        workflow = comparisonShortWorkflow(topic = "commonly confused everyday concepts"),
        category = Media,
        description = "",
        badges = listOf("AI", "Media")
    ),
    MascotPreview(
        label = "Mascot Preview (base + both directions)",
        workflow = mascotPreviewWorkflow(),
        category = Media,
        description = "Generates just the comparison-short mascot's base image and both Kontext-edited pointing directions — for inspecting/rerolling the mascot without running the full comparison pipeline.",
        badges = listOf("AI", "Media"),
    ),
    ImageShorts("AI Shorts (Image Motion)", imageShortsWorkflow, Media,
        "Generate FLUX keyframes, repeat them into scene clips, stitch, caption, and encode.", badges = listOf("AI", "Stable")),
    ImageMotionShort("Image Motion Short (Simple)", imageMotionShortWorkflow, Media,
        "3 reusable Flux scenes with niche-aware prompts, stitched into one clip. No outline/caption step.", badges = listOf("AI")),
    VideoShorts("AI Shorts (Video Motion)", videoShortsWorkflow, Media,
        "Generate motion-first scene clips with Wan, then stitch, caption, and encode.", badges = listOf("AI", "Stable")),
    ImageEdit("Image Edit", imageEditWorkflow, Media,
        "Resize an image and crop a region out of it.", badges = listOf("Stable")),
    Slideshow("Slideshow", slideshowWorkflow, Media,
        "Render a set of images into a video at a fixed frame rate.", badges = listOf("Stable")),
    ApiIngestion("API Ingestion", apiIngestionDemoWorkflow, DataAndIo,
        "Fetch JSON over HTTP, extract fields, and persist to a file.", badges = listOf("Stable", "HTTP")),
    ApiIngestionPro("API Ingestion Pro", productIngestionWorkflow, DataAndIo,
        "Normalize a live API payload, preview the shape, and persist the cleaned record.", badges = listOf("HTTP", "Stable", "Desktop")),
    AiPipeline("AI Pipeline", aiPipelineWorkflow, Examples,
        "Showcase of the AI node category.", badges = listOf("Demo")),
    GeometryPipeline("Geometry", geometryPipelineWorkflow, Examples,
        "Showcase of the geometry node category.", badges = listOf("Demo")),
    AutomationPipeline("Automation", automationPipelineWorkflow, Examples,
        "Showcase of the automation node category.", badges = listOf("Demo")),
    Styles("Styles", styleNodesDemoWorkflow, Examples,
        "Showcase of the three sample card styles.", badges = listOf("Demo")),
    ListOps("List Ops", listOpsDemoWorkflow, Examples,
        "zip → map → filter → reduce list operations.", badges = listOf("Demo")),
    Control("Control", controlDemoWorkflow, Examples,
        "Loop, branch, and merge control-flow nodes.", badges = listOf("Demo")),
    Text("Text", textDemoWorkflow, Examples,
        "Format, split, and regex text operations.", badges = listOf("Demo")),
    Types("Types", typesDemoWorkflow, Examples,
        "Schema, cast, and validate type utilities.", badges = listOf("Demo")),
    Io("I/O", ioDemoWorkflow, Examples,
        "HTTP request, file read, and file write nodes.", badges = listOf("Demo")),
    Subgraph("Subgraph", subgraphDemoWorkflow, Examples,
        "A reusable pipeline nested inside a subgraph node.", badges = listOf("Demo")),
    Script("Script", scriptDemoWorkflow, Examples,
        "Run inline Kotlin Script over upstream values.", badges = listOf("Demo")),
}
