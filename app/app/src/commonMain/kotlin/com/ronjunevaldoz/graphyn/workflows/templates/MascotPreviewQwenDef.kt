package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.plugins.shorts.DEFAULT_MASCOT_DESCRIPTION
import com.ronjunevaldoz.graphyn.plugins.shorts.MascotPointDirections
import com.ronjunevaldoz.graphyn.templates.SdLoraRef
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Same paths as DemoQwenImg2ImgDef.kt's qwenImg2ImgWorkflow (confirmed SIGSEGV-crashing on the
// local 12GB host — see that file's TODO(qwen-edit-crash) and docs/architecture/lessons.md).
// Testing here against Modal's bigger GPU to see if the VRAM-exhaustion-triggered graph-cut crash
// simply doesn't occur with more headroom.
private const val QWEN_EDIT_DIFFUSION = "/models/qwen/diffusion/qwen-image-edit-2511-Q2_K.gguf"
private const val QWEN_EDIT_TEXT_ENC = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
private const val QWEN_EDIT_MMPROJ = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct.mmproj-Q8_0.gguf"
private const val QWEN_EDIT_VAE = "/models/qwen/vae/qwen_image_vae.safetensors"
private const val QWEN_EDIT_LORA_4STEP = "/models/qwen/lora/Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors"

/**
 * Qwen-Image-Edit equivalent of [com.ronjunevaldoz.graphyn.plugins.shorts.mascotPointEditSubgraph]
 * — same shape (`ref_images` left as a free/unconnected boundary port the caller wires at
 * runtime), different model family. Built directly on `:templates`' `sdTxt2ImgWorkflow` instead of
 * hand-wiring nodes (unlike the FLUX version, which predates that shared builder) since this is a
 * one-off comparison test — is the comparison-short mascot's cross-direction identity drift a
 * FLUX-Kontext-specific limitation, or does Qwen-Image-Edit show the same pattern? — not a
 * permanent pipeline addition. Do not wire this into the real comparison-short pipeline without
 * first re-reading docs/architecture/lessons.md's Qwen-Image-Edit crash note; it's confirmed
 * broken on the local 12GB host and only being tried here against Modal's larger GPU.
 */
internal fun mascotPointEditQwenWorkflow(
    id: String,
    editInstruction: String,
    width: Int = SHORTS_WIDTH,
    height: Int = SHORTS_HEIGHT,
): WorkflowDefinition {
    val base = sdTxt2ImgWorkflow(
        id = id,
        name = "Mascot Point Edit (Qwen)",
        paths = SdModelPaths(
            diffusionModelPath = QWEN_EDIT_DIFFUSION,
            llmPath = QWEN_EDIT_TEXT_ENC,
            llmVisionPath = QWEN_EDIT_MMPROJ,
            vaePath = QWEN_EDIT_VAE,
            qwenImageZeroCondT = true,
        ),
        sampling = SdSamplingDefaults(
            steps = 4,
            cfgScale = 1.0,
            distilledGuidance = 1.0,
            flowShift = 12.0,
            sampleMethod = "euler",
            scheduler = "discrete",
            loras = listOf(SdLoraRef(path = QWEN_EDIT_LORA_4STEP, multiplier = 1.0)),
        ),
        prompt = editInstruction,
        negativePrompt = "",
        width = width,
        height = height,
    )
    // id_cond's ref_images is deliberately left unset here — sdTxt2ImgWorkflow already wires
    // id_cond into the generation node; the caller supplies ref_images at runtime via an outer
    // ConnectionRef, same free-boundary-port pattern as mascotPointEditSubgraph.
    return base.copy(
        nodes = base.nodes + NodeRef("import", "media.image_import") + NodeRef("preview", "preview.view"),
        connections = base.connections +
            ConnectionRef("generate", "image", "import", "path") +
            ConnectionRef("import", "image", "preview", "value"),
    )
}

/**
 * Same shape as [mascotPreviewWorkflow] but edits with Qwen-Image-Edit instead of FLUX Kontext —
 * reuses the SAME FLUX-generated base mascot (mascotSubgraph, already proven reliable) so only the
 * edit step's model family differs between this and the real mascot-preview workflow. That isolates
 * the comparison to "which model edits more reliably", not "which model's base generation differs".
 */
internal fun mascotPreviewQwenWorkflow(
    mascotDescription: String? = null,
    width: Int? = null,
    height: Int? = null,
): WorkflowDefinition {
    val mascotDescription = mascotDescription ?: DEFAULT_MASCOT_DESCRIPTION
    val width = width ?: SHORTS_WIDTH
    val height = height ?: SHORTS_HEIGHT
    return WorkflowDefinition(
        id = "mascot-preview-qwen",
        name = "Mascot Preview Qwen (base + both directions)",
        nodes = buildList {
            add(NodeRef(
                "mascotBase", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotSubgraph(id = "mascot-preview-qwen-base", mascotDescription = mascotDescription, width = width, height = height),
            ))
            add(NodeRef(
                "mascotLeft", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotPointEditQwenWorkflow(id = "mascot-preview-qwen-left", editInstruction = MascotPointDirections.POINT_LEFT, width = width, height = height),
            ))
            add(NodeRef(
                "mascotRight", SHORTS_SCENE_SUBGRAPH_NODE_TYPE,
                subgraph = mascotPointEditQwenWorkflow(id = "mascot-preview-qwen-right", editInstruction = MascotPointDirections.POINT_RIGHT, width = width, height = height),
            ))
            add(NodeRef("leftOutput", "media.file_output"))
            add(NodeRef("rightOutput", "media.file_output"))
        },
        connections = buildList {
            add(ConnectionRef("mascotBase", "video", "mascotLeft", "ref_images"))
            add(ConnectionRef("mascotBase", "video", "mascotLeft", "gate"))
            add(ConnectionRef("mascotBase", "video", "mascotRight", "ref_images"))
            add(ConnectionRef("mascotLeft", "video", "mascotRight", "gate"))
            add(ConnectionRef("mascotLeft", "video", "leftOutput", "file_path"))
            add(ConnectionRef("mascotRight", "video", "rightOutput", "file_path"))
        },
    )
}
