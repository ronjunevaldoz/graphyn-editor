package com.ronjunevaldoz.graphyn.plugins.shorts

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.booleanValue as b
import com.ronjunevaldoz.graphyn.core.model.doubleValue as d
import com.ronjunevaldoz.graphyn.core.model.intValue as i
import com.ronjunevaldoz.graphyn.core.model.stringValue as s

internal const val FLUX_DIFFUSION = "/models/flux/diffusion/flux1-schnell-Q4_K_S.gguf"
internal const val FLUX_CLIP_L = "/models/flux/text_encoder/clip_l.safetensors"
// t5xxl_Q5_K_M.gguf — the previous t5-v1_1-xxl-encoder-Q3_K_S.gguf doesn't exist on the Modal
// deployment's model volume (confirmed via /api/sd/models/exists) or in server-sd's own model
// catalog; Q5_K_M is what's actually uploaded there and to the Windows host. The larger quant is
// fine on Modal's L4 (far more VRAM headroom than the 12GB Windows card, especially at 480p).
internal const val FLUX_T5XXL = "/models/flux/text_encoder/t5xxl_Q5_K_M.gguf"
internal const val FLUX_VAE = "/models/flux/vae/ae.safetensors"
// Distinct diffusion checkpoint from base Flux schnell, same clip_l/t5xxl/vae encoders — see
// DemoFluxKontextImg2ImgDef.kt's KDoc. Used when useCharacterSheet=true or by
// imageMotionSceneEditSubgraph (ImageMotionSceneEdit.kt): base Flux has no reference-image
// conditioning capability at all, only Kontext does.
internal const val FLUX_KONTEXT_DIFFUSION = "/models/flux/diffusion/flux1-kontext-dev-Q4_K_S.gguf"

/**
 * One reusable image-motion scene: Flux txt2img (niche-aware prompt) rendered into an
 * [imageCount]-second clip via a Ken Burns pan/zoom instead of a flat static repeat. Call this
 * once per scene in a shorts template — it's a plain builder function, not a shared runtime
 * instance, so each call gets its own prompt.
 */
public fun imageMotionSceneSubgraph(
    prompt: String,
    niche: String,
    imageCount: Int = 2,
    visualStyle: String = "",
    character: String = "",
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useCharacterSheet: Boolean = false,
    imagePathSidecarPath: String? = null,
): WorkflowDefinition = imageMotionSceneSubgraphInternal(
    id = "image-motion-scene-${prompt.hashCode()}",
    imageCount = imageCount,
    promptEnhanceConfig = mapOf(
        "prompt" to s(prompt), "niche" to s(niche), "visual_style" to s(visualStyle), "character" to s(character),
    ),
    width = width,
    height = height,
    useCharacterSheet = useCharacterSheet,
    imagePathSidecarPath = imagePathSidecarPath,
)

/**
 * Same scene pipeline, but `prompt`/`niche` are left as unconnected boundary ports on the inner
 * `promptEnhance` node instead of literal config — the execution engine free-matches whatever the
 * outer subgraph instance receives on ports of the same name. Use this when a storyboard generator
 * supplies prompts/niche at runtime instead of Kotlin-build-time literals.
 *
 * [width]/[height] default to [ShortsConstants] but are not baked into the shared constant — a
 * lower-res call site (e.g. cheaper/faster iteration against a Modal deployment) can pass its own
 * values without changing every other consumer of [ShortsConstants.WIDTH]/[ShortsConstants.HEIGHT].
 *
 * [useCharacterSheet], when true, swaps the diffusion checkpoint to FLUX Kontext and adds an
 * `sd.id_cond` node whose `ref_images` port is left unconnected — a boundary port an outer
 * workflow wires a character-sheet reference image's `image` output into (see
 * `characterSheetSubgraphDynamic`) for cross-scene visual consistency. Costs materially more time
 * per scene (20 sampling steps instead of 4, plus reference-image conditioning) — keep the default
 * `false` (bare Ken Burns) for iteration on everything else in the pipeline, and only pay this
 * cost when actually validating character consistency.
 */
public fun imageMotionSceneSubgraphDynamic(
    id: String,
    imageCount: Int = 2,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useCharacterSheet: Boolean = false,
    imagePathSidecarPath: String? = null,
): WorkflowDefinition = imageMotionSceneSubgraphInternal(
    id = id, imageCount = imageCount, promptEnhanceConfig = emptyMap(),
    width = width, height = height, useCharacterSheet = useCharacterSheet,
    imagePathSidecarPath = imagePathSidecarPath,
)

private fun imageMotionSceneSubgraphInternal(
    id: String,
    imageCount: Int,
    promptEnhanceConfig: Map<String, WorkflowValue>,
    width: Int = ShortsConstants.WIDTH,
    height: Int = ShortsConstants.HEIGHT,
    useCharacterSheet: Boolean = false,
    imagePathSidecarPath: String? = null,
): WorkflowDefinition {
    require(imageCount >= 1) { "Scene duration must be at least 1 second, got $imageCount" }
    return WorkflowDefinition(
        id = id,
        name = "Image Motion Scene",
        nodes = buildList {
            add(NodeRef("diffusion", "sd.diffusion", config = mapOf(
                "diffusion_model_path" to s(if (useCharacterSheet) FLUX_KONTEXT_DIFFUSION else FLUX_DIFFUSION),
            )))
            add(NodeRef("encoders", "sd.encoders", config = mapOf("clip_l_path" to s(FLUX_CLIP_L), "t5xxl_path" to s(FLUX_T5XXL))))
            add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(FLUX_VAE))))
            add(NodeRef("model", "sd.model"))
            add(NodeRef("ctx", "sd.context", config = buildMap {
                put("diffusion_flash_attn", b(true))
                put("n_threads", i(-1))
                // Confirmed necessary for Kontext in DemoFluxKontextImg2ImgDef.kt — Kontext + its
                // encoders leave little VRAM headroom otherwise.
                if (useCharacterSheet) put("clip_on_cpu", b(true))
            }))
            add(NodeRef("promptEnhance", ShortsConstants.PROMPT_ENHANCE_NODE_TYPE, config = promptEnhanceConfig))
            if (useCharacterSheet) {
                // ref_images deliberately left unset — an unconnected boundary port an outer
                // workflow wires the character sheet's image output into (see SdBaseChain.kt's
                // sdBaseChain, which every other sd.* template always includes this same way).
                add(NodeRef("idCond", "sd.id_cond"))
            }
            add(NodeRef("sampler", "sd.sampler", config = mapOf(
                "sample_method" to s("euler"),
                "scheduler" to s("discrete"),
                // Kontext needs more steps than base Flux schnell's 4-step distillation — same
                // sampler config as the validated DemoFluxKontextImg2ImgDef.kt.
                "sample_steps" to i(if (useCharacterSheet) 20 else 4),
                "txt_cfg" to d(1.0),
                "distilled_guidance" to d(3.5),
                "flow_shift" to d(3.0),
            )))
            add(NodeRef("txt2img", "sd.txt2img", config = mapOf(
                "negative_prompt" to s(""),
                "width" to i(width),
                "height" to i(height),
                "seed" to i(-1),
                "batch_count" to i(1),
            )))
            if (imagePathSidecarPath != null) {
                // Persists the raw keyframe path so a later run (e.g. regenerate-scene's edit
                // mode) can condition a Kontext edit on this exact image — sd.txt2img's own
                // output file already survives permanently under ~/.graphyn/artifacts, but
                // nothing else records which scene it belongs to.
                add(NodeRef("imagePathSave", "io.file_write", config = mapOf(
                    "path" to s(imagePathSidecarPath), "append" to WorkflowValue.BooleanValue(false),
                )))
            }
            add(NodeRef("import", "media.image_import"))
            add(NodeRef("kenBurns", "media.ken_burns", config = mapOf(
                "duration_ms" to d(imageCount * 1000.0),
                "fps" to d(24.0),
                "zoom_start" to d(1.0),
                "zoom_end" to d(1.15),
                "pan_x" to s("center"),
                "pan_y" to s("center"),
                "width" to i(width),
                "height" to i(height),
            )))
            add(NodeRef("preview", "preview.view"))
        },
        connections = buildList {
            add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
            add(ConnectionRef("encoders", "encoders", "model", "encoders"))
            add(ConnectionRef("vae", "vae", "model", "vae"))
            add(ConnectionRef("model", "model", "ctx", "model"))
            add(ConnectionRef("promptEnhance", "prompt", "txt2img", "prompt"))
            add(ConnectionRef("promptEnhance", "negative_prompt", "txt2img", "negative_prompt"))
            add(ConnectionRef("ctx", "context", "txt2img", "context"))
            add(ConnectionRef("sampler", "sampler", "txt2img", "sampler"))
            if (useCharacterSheet) add(ConnectionRef("idCond", "id_cond", "txt2img", "id_cond"))
            if (imagePathSidecarPath != null) add(ConnectionRef("txt2img", "image", "imagePathSave", "content"))
            add(ConnectionRef("txt2img", "image", "import", "path"))
            add(ConnectionRef("import", "image", "kenBurns", "image"))
            add(ConnectionRef("kenBurns", "video", "preview", "value"))
        },
    )
}
