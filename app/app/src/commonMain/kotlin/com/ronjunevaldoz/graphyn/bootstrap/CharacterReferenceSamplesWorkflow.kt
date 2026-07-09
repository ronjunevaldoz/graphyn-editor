package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.core.model.ConnectionRef
import com.ronjunevaldoz.graphyn.core.model.NodeRef
import com.ronjunevaldoz.graphyn.core.model.WorkflowDefinition
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

private const val SDXL_DIFFUSION = "/models/sdxl/diffusion/sd_xl_base_1.0.safetensors"
private const val SDXL_VAE = "/models/sdxl/vae/sdxl_vae.safetensors"
private const val PHOTOMAKER_V2_WEIGHTS = "/models/sdxl/photomaker/photomaker-v2.safetensors"

private fun s(value: String) = WorkflowValue.StringValue(value)
private fun i(value: Int) = WorkflowValue.IntValue(value)
private fun d(value: Double) = WorkflowValue.DoubleValue(value)

/**
 * Default sample prompts — same character, varied pose/framing/setting, to visually validate
 * identity consistency across generations. Each uses PhotoMaker's required class-word + "img"
 * trigger-word convention (see stable-diffusion.cpp's docs/photo_maker.md).
 */
public object CharacterSamplePrompts {
    public val DEFAULT: List<String> = listOf(
        "a woman img, portrait photo, studio lighting, neutral background",
        "a woman img, three-quarter view, walking in a city street, natural light",
        "a woman img, close-up, warm genuine smile, golden hour lighting",
        "a woman img, retro futurism poster art, intricate details, masterpiece",
    )
}

/** One-time setup instructions for producing the id_embeds.bin this workflow requires. */
public val faceDetectInstructions: String = """
    One-time setup per character (run locally, not part of this workflow):
      1. Collect 3-10 clear photos of the character's face into one folder.
      2. pip install torch diffusers insightface==0.7.3 safetensors
      3. python native/stable-diffusion.cpp/script/face_detect.py <folder_of_photos>
      4. Writes id_embeds.bin into that folder (CPU-only, ~1-2 min). Reuse it
         for every future generation of this character - no need to rerun.
      5. Pass its path as idEmbedPath (CLI: id_embed_path=...).
""".trimIndent()

/**
 * Generates [prompts].size SDXL+PhotoMaker-v2 images from a single character's precomputed
 * [idEmbedPath] (an `id_embeds.bin` produced by `native/stable-diffusion.cpp/script/face_detect.py`
 * — see [faceDetectInstructions]), one per prompt, so a user can visually check identity
 * consistency before committing to a full shorts pipeline.
 *
 * [idEmbedPath] is a required function parameter, not a CLI `nodeId.port=value` override — this
 * workflow builds N `sd.txt2img` nodes dynamically (one per sample), which
 * `WorkflowCliRunner`'s override mechanism only supports for fixed [WorkflowCatalog] entries, not
 * per-invocation dynamically-sized graphs (same shape as the `storyboard`/`regenerate-scene`
 * modes' own per-scene loops).
 *
 * Hand-built rather than via `:templates`' `sdTxt2ImgWorkflow` for two reasons: it fans out to N
 * generation nodes sharing one model/context/id_cond, and SDXL needs no `sd.encoders` node at all
 * (its CLIP-L/CLIP-G are baked into the checkpoint, loaded via `sd.diffusion`'s `model_path` —
 * see [photoMakerSdxlWorkflow]'s doc for why `model_path`, not `diffusion_model_path`), unlike
 * `sdBaseChain`'s FLUX/Qwen-oriented default which always emits one.
 */
public fun characterReferenceSamplesWorkflow(
    idEmbedPath: String,
    prompts: List<String> = CharacterSamplePrompts.DEFAULT,
    negativePrompt: String = "realistic, photo-realistic, worst quality, greyscale, bad anatomy, bad hands, error, text",
    styleStrength: Double = 20.0,
    steps: Int = 30,
    cfgScale: Double = 5.0,
    width: Int = 1024,
    height: Int = 1024,
): WorkflowDefinition {
    require(idEmbedPath.isNotBlank()) { "idEmbedPath is required. $faceDetectInstructions" }

    val nodes = buildList {
        add(NodeRef("diffusion", "sd.diffusion", config = mapOf("model_path" to s(SDXL_DIFFUSION))))
        add(NodeRef("vae", "sd.vae", config = mapOf("vae_path" to s(SDXL_VAE))))
        add(NodeRef("model", "sd.model", config = mapOf("photo_maker_path" to s(PHOTOMAKER_V2_WEIGHTS))))
        add(NodeRef("context", "sd.context", config = mapOf("diffusion_flash_attn" to WorkflowValue.BooleanValue(true))))
        add(
            NodeRef(
                "id_cond", "sd.id_cond",
                config = mapOf(
                    "pm_id_embed_path" to s(idEmbedPath),
                    "pm_style_strength" to d(styleStrength),
                ),
            ),
        )
        add(
            NodeRef(
                "sampler", "sd.sampler",
                config = mapOf(
                    "sample_method" to s("euler"),
                    "scheduler" to s("discrete"),
                    "sample_steps" to i(steps),
                    "txt_cfg" to d(cfgScale),
                ),
            ),
        )
        prompts.forEachIndexed { index, prompt ->
            add(
                NodeRef(
                    "sample$index", "sd.txt2img",
                    config = mapOf(
                        "prompt" to s(prompt),
                        "negative_prompt" to s(negativePrompt),
                        "width" to i(width),
                        "height" to i(height),
                        "seed" to i(-1),
                        "batch_count" to i(1),
                    ),
                ),
            )
            add(NodeRef("preview$index", "preview.view"))
        }
    }

    val connections = buildList {
        add(ConnectionRef("diffusion", "diffusion", "model", "diffusion"))
        add(ConnectionRef("vae", "vae", "model", "vae"))
        add(ConnectionRef("model", "model", "context", "model"))
        prompts.indices.forEach { index ->
            add(ConnectionRef("context", "context", "sample$index", "context"))
            add(ConnectionRef("sampler", "sampler", "sample$index", "sampler"))
            add(ConnectionRef("id_cond", "id_cond", "sample$index", "id_cond"))
            add(ConnectionRef("sample$index", "image", "preview$index", "value"))
        }
    }

    return WorkflowDefinition(
        id = "character-reference-samples",
        name = "Character Reference → Generate Samples",
        nodes = nodes + guideNote(
            "Character Reference -> Generate Samples\n\n$faceDetectInstructions",
            width = 340,
            height = 320,
        ),
        connections = connections,
    )
}
