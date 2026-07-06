package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue

/**
 * The canonical `sd.*` text-to-image graph — the same node wiring for every model family
 * (Flux, Qwen-Image, SD1.5/SDXL, ...), which differ only in [paths] and [sampling]. Also supports
 * reference-image conditioning (Qwen-Image-Edit, PhotoMaker, PuLID) via the base chain's always-
 * present `sd.id_cond` node — override its `ref_images` config per-request to activate it.
 *
 * Node graph: `sd.diffusion` + `sd.encoders` + `sd.vae` → `sd.model` → `sd.context` + `sd.id_cond`
 * → (optional `sd.lora` chain →) `sd.sampler` → `sd.txt2img` (node id `"generate"`).
 *
 * [prompt]/[negativePrompt]/[width]/[height]/[seed] are the per-run fields — a caller running
 * this as a stored workflow (`POST /workflows/{id}/run`) overrides node id `"generate"`'s ports
 * with those values; everything else (model paths, sampler defaults) is fixed per model family.
 */
fun sdTxt2ImgWorkflow(
    id: String,
    name: String,
    paths: SdModelPaths,
    sampling: SdSamplingDefaults,
    prompt: String = "",
    negativePrompt: String = "",
    width: Int = 512,
    height: Int = 512,
    seed: Int = -1,
): WorkflowDefinition {
    val (baseNodes, baseConnections) = sdBaseChain(paths, sampling)
    val generationNode = NodeRef(
        id = GENERATION_NODE_ID,
        type = "sd.txt2img",
        config = mapOf(
            "prompt" to StringValue(prompt),
            "negative_prompt" to StringValue(negativePrompt),
            "width" to IntValue(width),
            "height" to IntValue(height),
            "seed" to IntValue(seed),
            "batch_count" to IntValue(1),
        ),
    )

    return WorkflowDefinition(
        id = id,
        name = name,
        nodes = baseNodes + generationNode,
        connections = baseConnections + sdGenerationNodeConnections(paths),
    )
}
