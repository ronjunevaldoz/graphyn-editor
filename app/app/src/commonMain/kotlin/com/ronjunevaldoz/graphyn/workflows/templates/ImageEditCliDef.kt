package com.ronjunevaldoz.graphyn.workflows

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.templates.SdLoraRef
import com.ronjunevaldoz.graphyn.templates.SdModelPaths
import com.ronjunevaldoz.graphyn.templates.SdSamplingDefaults
import com.ronjunevaldoz.graphyn.templates.sdTxt2ImgWorkflow

// Same paths as DemoFluxKontextImg2ImgDef.kt's fluxKontextImg2ImgWorkflow.
private const val FLUX_KONTEXT_DIFFUSION = "/models/flux/diffusion/flux1-kontext-dev-Q4_K_S.gguf"
private const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
private const val FLUX_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
private const val FLUX_VAE = "/models/flux/vae/ae.safetensors"

// Same paths as DemoQwenImg2ImgDef.kt's qwenImg2ImgWorkflow.
private const val QWEN_EDIT_DIFFUSION = "/models/qwen/diffusion/qwen-image-edit-2511-Q2_K.gguf"
private const val QWEN_EDIT_TEXT_ENC = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct-UD-Q4_K_XL.gguf"
private const val QWEN_EDIT_MMPROJ = "/models/qwen/text_encoder/Qwen2.5-VL-7B-Instruct.mmproj-Q8_0.gguf"
private const val QWEN_EDIT_VAE = "/models/qwen/vae/qwen_image_vae.safetensors"
private const val QWEN_EDIT_LORA_4STEP = "/models/qwen/lora/Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors"

/**
 * Wires an `sd.offload` node into `context`'s `offload` port. `sd.context`'s own `clip_on_cpu`/
 * `vae_on_cpu`/`offload_params_to_cpu` ports were moved onto this sub-node (see
 * `SdContextComputePorts.kt`'s comment) — setting them via `withNodeConfig(nodeId = "context", ...)`
 * is a silent no-op since `sd.context` no longer has those keys in its own spec.
 */
private fun WorkflowDefinition.withOffloadToCpu(
    offloadParamsToCpu: Boolean = false,
    clipOnCpu: Boolean = false,
    vaeOnCpu: Boolean = false,
): WorkflowDefinition = copy(
    nodes = nodes + NodeRef(
        "offload", "sd.offload",
        config = mapOf(
            "offload_params_to_cpu" to WorkflowValue.BooleanValue(offloadParamsToCpu),
            "clip_on_cpu" to WorkflowValue.BooleanValue(clipOnCpu),
            "vae_on_cpu" to WorkflowValue.BooleanValue(vaeOnCpu),
        ),
    ),
    connections = connections + ConnectionRef("offload", "offload", "context", "offload"),
)

/**
 * CLI-parameterized equivalent of [fluxKontextImg2ImgWorkflow]/[qwenImg2ImgWorkflow] — same model
 * paths/sampling, but [imagePath]/[instruction] are runtime arguments instead of build-time
 * constants, so a reference image + edit description can be tried ad hoc via
 * `workflow=image-edit image=<path> instruction=<text> model=flux-kontext|qwen` without touching
 * source. Does not modify the existing DemoFluxKontextImg2ImgDef.kt/DemoQwenImg2ImgDef.kt gallery
 * demos, which stay fixed-sample for the launcher catalog.
 */
internal fun referenceImageEditWorkflow(
    imagePath: String,
    instruction: String,
    model: String,
    width: Int? = null,
    height: Int? = null,
    // Qwen-only knobs: the 4-step Lightning LoRA (default) needs steps=4/cfg=1.0 to behave —
    // set useLightningLora=false to run the base Qwen-Image-Edit model at conventional
    // steps/cfg instead (e.g. steps=15, cfgScale=2.5), which redraws less of the source pixels.
    steps: Int? = null,
    cfgScale: Double? = null,
    useLightningLora: Boolean? = null,
    seed: Int? = null,
    negativePrompt: String? = null,
): WorkflowDefinition {
    val width = width ?: 512
    val height = height ?: 512
    val useLightningLora = useLightningLora ?: true
    val seed = seed ?: -1
    val negativePrompt = negativePrompt ?: ""
    return when (model) {
    "flux-kontext" -> sdTxt2ImgWorkflow(
        id = "image-edit-flux-kontext",
        name = "Image Edit (FLUX Kontext)",
        paths = SdModelPaths(
            diffusionModelPath = FLUX_KONTEXT_DIFFUSION,
            clipLPath = FLUX_CLIP_L,
            t5xxlPath = FLUX_T5XXL,
            vaePath = FLUX_VAE,
        ),
        sampling = SdSamplingDefaults(
            steps = steps ?: 20, cfgScale = cfgScale ?: 1.0, distilledGuidance = 3.5, flowShift = 3.0,
            sampleMethod = "euler", scheduler = "discrete",
        ),
        prompt = instruction,
        negativePrompt = negativePrompt,
        width = width,
        height = height,
        seed = seed,
    ).withOffloadToCpu(clipOnCpu = true)
        .withNodeConfig(
            nodeId = "id_cond",
            overrides = mapOf("ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(imagePath)))),
        ).withGalleryPreview(outputPort = "image", guideText = "Image Edit (FLUX Kontext) · ad hoc CLI test")

    "qwen" -> sdTxt2ImgWorkflow(
        id = "image-edit-qwen",
        name = "Image Edit (Qwen-Image-Edit)",
        paths = SdModelPaths(
            diffusionModelPath = QWEN_EDIT_DIFFUSION,
            llmPath = QWEN_EDIT_TEXT_ENC,
            llmVisionPath = QWEN_EDIT_MMPROJ,
            vaePath = QWEN_EDIT_VAE,
            qwenImageZeroCondT = true,
        ),
        sampling = SdSamplingDefaults(
            steps = steps ?: if (useLightningLora) 4 else 15,
            cfgScale = cfgScale ?: if (useLightningLora) 1.0 else 2.5,
            // flow_shift=12 is tuned for the 4-step Lightning LoRA path; the base Qwen-Image-Edit
            // 2511 model's own documented recipe uses flow_shift=3 (leejet/stable-diffusion.cpp
            // docs/qwen_image_edit.md).
            distilledGuidance = 1.0, flowShift = if (useLightningLora) 12.0 else 3.0,
            sampleMethod = "euler", scheduler = "discrete",
            loras = if (useLightningLora) listOf(SdLoraRef(path = QWEN_EDIT_LORA_4STEP, multiplier = 1.0)) else emptyList(),
        ),
        prompt = instruction,
        negativePrompt = negativePrompt,
        width = width,
        height = height,
        seed = seed,
    ).withOffloadToCpu(offloadParamsToCpu = true)
        .withNodeConfig(
            nodeId = "id_cond",
            overrides = mapOf("ref_images" to WorkflowValue.ListValue(listOf(WorkflowValue.StringValue(imagePath)))),
        ).withGalleryPreview(outputPort = "image", guideText = "Image Edit (Qwen-Image-Edit) · ad hoc CLI test")

    else -> error("Unknown model '$model'. Use flux-kontext or qwen.")
    }
}

/**
 * Editor-catalog variant of [referenceImageEditWorkflow] — same model paths/sampling, but instead
 * of a build-time [imagePath] argument, adds an `io.file_browse` node wired into `id_cond`'s
 * `ref_images` port. That node renders as a native file-picker card in the editor (see
 * `plugins/io/.../IoEditorPlugin.kt`'s `FileBrowseCardFactory`) — clicking "Browse" sets its
 * `path` output, which flows straight into `ref_images` (list-typed, so the single connection
 * auto-wraps — same mechanism as every other `ref_images` wiring this session). Lets a user swap
 * the reference image visually instead of typing/pasting a path into a config field.
 */
internal fun referenceImageEditEditorWorkflow(instruction: String, model: String): WorkflowDefinition {
    val base = referenceImageEditWorkflow(imagePath = "", instruction = instruction, model = model)
    // referenceImageEditWorkflow always sets ref_images via withNodeConfig — drop that literal
    // config value so the browse node's connection is what actually supplies it.
    val withoutRefImagesConfig = base.copy(
        nodes = base.nodes.map { node ->
            if (node.id == "id_cond") node.copy(config = node.config - "ref_images") else node
        },
    )
    return withoutRefImagesConfig.copy(
        nodes = withoutRefImagesConfig.nodes + NodeRef("browse", "io.file_browse"),
        connections = withoutRefImagesConfig.connections + ConnectionRef("browse", "path", "id_cond", "ref_images"),
    )
}
