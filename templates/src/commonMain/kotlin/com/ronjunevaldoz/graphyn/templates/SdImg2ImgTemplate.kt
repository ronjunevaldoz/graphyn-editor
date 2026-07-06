package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue

/**
 * The canonical `sd.*` image-to-image graph (denoise-strength editing, e.g. Flux img2img) — same
 * base chain as [sdTxt2ImgWorkflow], generation node is `sd.img2img` instead (requires an
 * `init_image` path — always present, since the node spec's `init_image` port is non-nullable;
 * override it per-request via `POST /workflows/{id}/run`, same as `prompt`/`negativePrompt`/etc).
 *
 * For instruction-based edit models (Qwen-Image-Edit) that take the source as a *reference* image
 * rather than a denoise-strength init image, use [sdTxt2ImgWorkflow] with its `sd.id_cond` node's
 * `ref_images` overridden instead — this template is for classic img2img only.
 */
fun sdImg2ImgWorkflow(
    id: String,
    name: String,
    paths: SdModelPaths,
    sampling: SdSamplingDefaults,
    prompt: String = "",
    negativePrompt: String = "",
    width: Int = 512,
    height: Int = 512,
    seed: Int = -1,
    initImagePath: String = "",
    strength: Double = 0.6,
): WorkflowDefinition {
    val (baseNodes, baseConnections) = sdBaseChain(paths, sampling)
    val generationNode = NodeRef(
        id = GENERATION_NODE_ID,
        type = "sd.img2img",
        config = mapOf(
            "prompt" to StringValue(prompt),
            "negative_prompt" to StringValue(negativePrompt),
            "width" to IntValue(width),
            "height" to IntValue(height),
            "seed" to IntValue(seed),
            "batch_count" to IntValue(1),
            "init_image" to StringValue(initImagePath),
            "strength" to DoubleValue(strength),
        ),
    )

    return WorkflowDefinition(
        id = id,
        name = name,
        nodes = baseNodes + generationNode,
        connections = baseConnections + sdGenerationNodeConnections(paths),
    )
}
