package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

/**
 * The plain (non-edit) FLUX generation chain shared by [mascotSubgraph] and
 * [comparisonImageSubgraph] — `diffusion -> encoders -> vae -> model -> ctx -> promptEnhance ->
 * sampler -> txt2img`, previously defined twice with identical wiring and identical checkpoint
 * paths (this file reuses [FLUX_DIFFUSION]/[FLUX_CLIP_L]/[FLUX_T5XXL]/[FLUX_VAE], already
 * `internal` in `ImageMotionScene.kt`, instead of adding a third copy).
 *
 * [prompt]/[lighting]/[details] are nullable: pass a value to bake it into `promptEnhance`'s
 * config at Kotlin-build time (e.g. [mascotSubgraph]'s fixed mascot description); leave null to
 * keep that port unconnected so it resolves via the free-boundary-port mechanism from a caller's
 * runtime-extracted value (e.g. [comparisonImageSubgraph]'s per-pair prompt, only known once the
 * generated comparison JSON exists).
 *
 * [importResult] toggles whether the raw `txt2img` output is converted to a real image handle via
 * `media.image_import` before the free `value` output ([comparisonImageSubgraph] needs a handle for
 * `media.comparison_layout`) or left as a raw path ([mascotSubgraph] needs the raw path for
 * `sd.id_cond`'s list-typed `ref_images` port — importing it here would break that downstream cast).
 */
public fun fluxPlainGenerationSubgraph(
    id: String,
    name: String = "Flux Generation",
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    prompt: String? = null,
    lighting: String? = null,
    details: String? = null,
    seed: Int = -1,
    useLlmPromptEnhance: Boolean = false,
    importResult: Boolean = false,
): WorkflowDefinition = WorkflowDefinition(
    id = id,
    name = name,
    nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("diffusion_model_path" to s(FLUX_DIFFUSION))))
        add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(FLUX_VAE))))
        add(NodeRef("model", "sd.model"))
        add(NodeRef("ctx", "sd.context", config = mapOf("diffusion_flash_attn" to b(true), "n_threads" to i(-1))))
        if (useLlmPromptEnhance) {
            add(NodeRef("promptEnhance", ShortsNodeTypes.PROMPT_ENHANCE_LLM, config = buildMap {
                if (prompt != null) put("prompt", s(listOfNotNull(prompt, lighting, details).joinToString(", ")))
            }))
            // Same pattern as storyboardGeneratorSubgraph/comparisonGeneratorSubgraph — read the
            // real deployed model name/host from the environment instead of relying on this node's
            // own internal fallbacks, which aren't this project's actual Ollama deployment.
            add(NodeRef("ollamaModel", "env.read", config = mapOf("name" to s("GRAPHYN_OLLAMA_MODEL"))))
            add(NodeRef("ollamaHost", "env.read", config = mapOf("name" to s("GRAPHYN_OLLAMA_HOST"))))
        } else {
            add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE, config = buildMap {
                if (prompt != null) put("prompt", s(prompt))
                if (lighting != null) put("lighting", s(lighting))
                if (details != null) put("details", s(details))
            }))
        }
        add(NodeRef("sampler", "sd.sampler", config = mapOf(
            "sample_method" to s("euler"),
            "scheduler" to s("discrete"),
            "sample_steps" to i(4),
            "txt_cfg" to d(1.0),
            "distilled_guidance" to d(3.5),
            "flow_shift" to d(3.0),
        )))
        add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
            "negative_prompt" to s(""),
            "width" to i(width),
            "height" to i(height),
            "seed" to i(seed),
            "batch_count" to i(1),
        )))
        if (importResult) add(NodeRef("import", "media.image_import"))
        add(NodeRef("preview", "preview.view"))
    },
    connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("encoders", "encoders", "model", "encoders"))
        add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "ctx", "model"))
        if (useLlmPromptEnhance) {
            add(ConnectionRef("ollamaModel", "value", "promptEnhance", "model"))
            add(ConnectionRef("ollamaHost", "value", "promptEnhance", "host"))
        }
        add(ConnectionRef("promptEnhance", "prompt", "txt2img", "prompt"))
        add(ConnectionRef("promptEnhance", "negative_prompt", "txt2img", "negative_prompt"))
        add(ConnectionRef("ctx", "context", "txt2img", "context"))
        add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
        if (importResult) {
            add(ConnectionRef("txt2img", "image", "import", "path"))
            add(ConnectionRef("import", "image", "preview", "value"))
        } else {
            // SHORTS_SCENE_SUBGRAPH_NODE_TYPE's wrapper executor reads the subgraph's free output by
            // the fixed key "value" — without this terminal passthrough, the free output stays keyed
            // "image" (txt2img's own port name) and the wrapper silently resolves to NullValue.
            add(ConnectionRef("txt2img", "image", "preview", "value"))
        }
    },
)
