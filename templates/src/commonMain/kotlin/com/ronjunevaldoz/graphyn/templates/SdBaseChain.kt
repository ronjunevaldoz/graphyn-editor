package com.ronjunevaldoz.graphyn.templates

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.BooleanValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue

/**
 * The `sd.diffusion`/`sd.encoders`/`sd.vae` → `sd.model` → `sd.context` → (`sd.lora` chain →)
 * `sd.sampler` chain shared by every `sd.*` generation template (txt2img, img2img, ...) — the
 * part that only depends on the model family, not on the specific generation mode.
 *
 * Also always includes an `sd.id_cond` node (ref images / PhotoMaker / PuLID), inert by default
 * (`ref_images = null`) — wired to the generation node's `id_cond` port so a caller running this
 * as a stored workflow can activate reference-image conditioning per-request by overriding just
 * the `id_cond` node's `ref_images` config, without the stored graph needing new nodes/wiring at
 * request time (`POST /workflows/{id}/run` overrides can only edit existing nodes' config).
 */
internal fun sdBaseChain(paths: SdModelPaths, sampling: SdSamplingDefaults): Pair<List<NodeRef>, List<ConnectionRef>> {
    val nodes = buildList {
        add(
            NodeRef(
                id = "diffusion",
                type = "sd.diffusion",
                config = mapOf("diffusion_model_path" to StringValue(paths.diffusionModelPath)),
            ),
        )
        add(
            NodeRef(
                id = "encoders",
                type = "sd.encoders",
                config = buildMap {
                    paths.clipLPath?.let { put("clip_l_path", StringValue(it)) }
                    paths.clipGPath?.let { put("clip_g_path", StringValue(it)) }
                    paths.t5xxlPath?.let { put("t5xxl_path", StringValue(it)) }
                    paths.llmPath?.let { put("llm_path", StringValue(it)) }
                    paths.llmVisionPath?.let { put("llm_vision_path", StringValue(it)) }
                },
            ),
        )
        if (paths.vaePath != null) {
            add(NodeRef(id = "vae", type = "sd.vae", config = mapOf("vae_path" to StringValue(paths.vaePath))))
        }
        add(NodeRef(id = "model", type = "sd.model"))
        add(
            NodeRef(
                id = "context",
                type = "sd.context",
                config = buildMap {
                    put("n_threads", IntValue(paths.nThreads))
                    put("enable_mmap", BooleanValue(paths.enableMmap))
                    put("diffusion_flash_attn", BooleanValue(paths.diffusionFa))
                    put("qwen_image_zero_cond_t", BooleanValue(paths.qwenImageZeroCondT))
                    paths.backend?.let { put("backend", StringValue(it)) }
                },
            ),
        )
        add(NodeRef(id = "id_cond", type = "sd.id_cond", config = mapOf("ref_images" to NullValue)))
        sampling.loras.forEachIndexed { index, lora ->
            add(
                NodeRef(
                    id = "lora$index",
                    type = "sd.lora",
                    config = mapOf(
                        "path" to StringValue(lora.path),
                        "multiplier" to DoubleValue(lora.multiplier),
                    ),
                ),
            )
        }
        add(
            NodeRef(
                id = "sampler",
                type = "sd.sampler",
                config = buildMap {
                    put("sample_steps", IntValue(sampling.steps))
                    put("txt_cfg", DoubleValue(sampling.cfgScale))
                    sampling.distilledGuidance?.let { put("distilled_guidance", DoubleValue(it)) }
                    sampling.flowShift?.let { put("flow_shift", DoubleValue(it)) }
                    sampling.sampleMethod?.let { put("sample_method", StringValue(it)) }
                    sampling.scheduler?.let { put("scheduler", StringValue(it)) }
                },
            ),
        )
    }

    val connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        if (paths.vaePath != null) add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "context", "model"))
        sampling.loras.indices.forEach { index -> add(ConnectionRef("lora$index", "lora", GENERATION_NODE_ID, "loras")) }
    }

    return nodes to connections
}

/**
 * Connections from the shared base chain into the generation node (`sd.txt2img`/`sd.img2img`).
 * Every generation node uses these same three input port names, so this is shared by both
 * templates.
 */
internal fun sdGenerationNodeConnections(paths: SdModelPaths): List<ConnectionRef> = buildList {
    add(ConnectionRef("context", "context", GENERATION_NODE_ID, "context"))
    add(ConnectionRef("sampler", "sampler", GENERATION_NODE_ID, "sampler"))
    add(ConnectionRef("id_cond", "id_cond", GENERATION_NODE_ID, "id_cond"))
}

/** Node id shared by both templates' final `sd.txt2img`/`sd.img2img` node. */
internal const val GENERATION_NODE_ID = "generate"
