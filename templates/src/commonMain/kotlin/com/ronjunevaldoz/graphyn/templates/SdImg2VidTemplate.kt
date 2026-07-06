package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue

/**
 * The canonical `sd.img2vid` graph for MoE video models (Wan2.1/Wan2.2): `sd.diffusion` (low +
 * optional high-noise weights) + `sd.encoders` (CLIP-vision + text encoder) + `sd.vae` →
 * `sd.model` → `sd.context` (+ optional `sd.offload`) + `sd.vae_tiling` → `sd.sampler`
 * (+ optional `sd.sampler` for the high-noise pass) → `sd.img2vid` (node id `"generate"`).
 *
 * Distinct from the image templates' `sdBaseChain` — video has its own MoE dual-sampler and
 * offload/tiling shape that doesn't fit the image chain without conditionals leaking both ways.
 *
 * [prompt]/[negativePrompt]/[width]/[height]/[seed]/[initImagePath] are the per-run fields —
 * override node id `"generate"`'s ports via `POST /workflows/{id}/run`.
 */
fun sdImg2VidWorkflow(
    id: String,
    name: String,
    paths: SdVideoModelPaths,
    sampling: SdVideoSamplingDefaults,
    prompt: String = "",
    negativePrompt: String = "",
    width: Int = 832,
    height: Int = 480,
    seed: Int = -1,
    videoFrames: Int = 81,
    fps: Int = 16,
    initImagePath: String = "",
): WorkflowDefinition {
    val nodes = buildList {
        add(
            NodeRef(
                id = "diffusion",
                type = "sd.diffusion",
                config = buildMap {
                    put("diffusion_model_path", StringValue(paths.lowNoiseModelPath))
                    paths.highNoiseModelPath?.let { put("high_noise_diffusion_model_path", StringValue(it)) }
                },
            ),
        )
        add(
            NodeRef(
                id = "encoders",
                type = "sd.encoders",
                config = buildMap {
                    paths.clipVisionPath?.let { put("clip_vision_path", StringValue(it)) }
                    paths.textEncoderPath?.let { put("t5xxl_path", StringValue(it)) }
                },
            ),
        )
        if (paths.vaePath != null) {
            add(NodeRef(id = "vae", type = "sd.vae", config = mapOf("vae_path" to StringValue(paths.vaePath))))
        }
        add(NodeRef(id = "model", type = "sd.model"))
        if (paths.maxVram != null) {
            add(NodeRef(id = "offload", type = "sd.offload", config = mapOf("max_vram" to DoubleValue(paths.maxVram.toDoubleOrNull() ?: -1.0))))
        }
        add(
            NodeRef(
                id = "context",
                type = "sd.context",
                config = buildMap {
                    put("n_threads", IntValue(paths.nThreads))
                    put("diffusion_flash_attn", BooleanValue(paths.diffusionFa))
                    paths.backend?.let { put("backend", StringValue(it)) }
                },
            ),
        )
        add(
            NodeRef(
                id = "vae_tiling",
                type = "sd.vae_tiling",
                config = mapOf(
                    "enabled" to BooleanValue(paths.vaeTiling),
                    "temporal_tiling" to BooleanValue(paths.vaeTilingTemporal),
                ),
            ),
        )
        add(
            NodeRef(
                id = "sampler",
                type = "sd.sampler",
                config = buildMap {
                    put("sample_steps", IntValue(sampling.steps))
                    put("txt_cfg", DoubleValue(sampling.cfgScale))
                    sampling.flowShift?.let { put("flow_shift", DoubleValue(it)) }
                },
            ),
        )
        sampling.highNoiseSteps?.let { highNoiseSteps ->
            add(
                NodeRef(
                    id = "high_noise_sampler",
                    type = "sd.sampler",
                    config = mapOf("sample_steps" to IntValue(highNoiseSteps)),
                ),
            )
        }
        add(
            NodeRef(
                id = GENERATION_NODE_ID,
                type = "sd.img2vid",
                config = mapOf(
                    "prompt" to StringValue(prompt),
                    "negative_prompt" to StringValue(negativePrompt),
                    "width" to IntValue(width),
                    "height" to IntValue(height),
                    "seed" to IntValue(seed),
                    "video_frames" to IntValue(videoFrames),
                    "fps" to IntValue(fps),
                    "moe_boundary" to DoubleValue(sampling.moeBoundary),
                    "init_image" to StringValue(initImagePath),
                ),
            ),
        )
    }

    val connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        if (paths.vaePath != null) add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "context", "model"))
        if (paths.maxVram != null) add(ConnectionRef("offload", "offload", "context", "offload"))
        add(ConnectionRef("context", "context", GENERATION_NODE_ID, "context"))
        add(ConnectionRef("vae_tiling", "vae_tiling", GENERATION_NODE_ID, "vae_tiling"))
        add(ConnectionRef("sampler", "sampler", GENERATION_NODE_ID, "sampler"))
        if (sampling.highNoiseSteps != null) {
            add(ConnectionRef("high_noise_sampler", "sampler", GENERATION_NODE_ID, "high_noise_sampler"))
        }
    }

    return WorkflowDefinition(id = id, name = name, nodes = nodes, connections = connections)
}
