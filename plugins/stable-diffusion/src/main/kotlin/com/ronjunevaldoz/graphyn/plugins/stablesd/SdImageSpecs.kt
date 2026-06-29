package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.ListType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Shared input ports for both image generation nodes (from sd_img_gen_params_t). */
private val imageGenSharedPorts = listOf(
    PortSpec("context", OpaqueType, portColor = COLOR_MODEL,
        description = "SD model context from sd.context node."),
    PortSpec("sampler", OpaqueType, portColor = COLOR_SAMPLER,
        description = "Sampling config from sd.sampler node."),
    PortSpec("prompt", StringType, portColor = COLOR_STRING,
        description = "--prompt: Positive conditioning text. Supports <lora:name:weight> inline LoRA syntax."),
    PortSpec("negative_prompt", StringType, portColor = COLOR_STRING,
        description = "--negative-prompt: Negative conditioning text. Empty = uncond."),
    PortSpec("width", IntType, portColor = COLOR_INT,
        description = "--width: Output width in pixels. -1 = model default."),
    PortSpec("height", IntType, portColor = COLOR_INT,
        description = "--height: Output height in pixels. -1 = model default."),
    PortSpec("seed", IntType, portColor = COLOR_INT,
        description = "--seed: RNG seed. -1 = random per batch item."),
    PortSpec("batch_count", IntType, portColor = COLOR_INT,
        description = "--batch-count: Number of images to generate in one call. Default: 1."),
    PortSpec("clip_skip", IntType, portColor = COLOR_INT,
        description = "--clip-skip: CLIP layers to skip from the end. -1 = model default."),
    PortSpec("loras", NullableType(ListType(OpaqueType)), portColor = COLOR_LORA,
        description = "List of sd.lora tokens to apply. Null = none."),
    PortSpec("control_image", NullableType(StringType), portColor = COLOR_IMAGE,
        description = "--control-image: Path to ControlNet conditioning image (SD1.5 only)."),
    PortSpec("control_strength", DoubleType, portColor = COLOR_FLOAT,
        description = "--control-strength: ControlNet influence weight. Default: 0.9."),
    PortSpec("ref_images", NullableType(ListType(StringType)), portColor = COLOR_IMAGE,
        description = "--ref-image: Paths to reference images (PhotoMaker/PuLID/Qwen). Null = none."),
    PortSpec("auto_resize_ref_image", BooleanType, portColor = COLOR_BOOL,
        description = "--disable-auto-resize-ref-image: Auto-resize reference images to match width/height. Default: true."),
    PortSpec("increase_ref_index", BooleanType, portColor = COLOR_BOOL,
        description = "--increase-ref-index: Increment reference image index per batch item."),
    PortSpec("mask_image", NullableType(StringType), portColor = COLOR_IMAGE,
        description = "--mask: Inpainting mask path (white = inpaint, black = preserve). Null = no mask."),
    PortSpec("pm_id_embed_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--pm-id-embed-path: PhotoMaker id-embedding file path. Null = not used."),
    PortSpec("pm_id_images_dir", NullableType(StringType), portColor = COLOR_STRING,
        description = "--pm-id-images-dir: Directory of PhotoMaker id-images (alternative to pm_id_embed_path). Null = not used."),
    PortSpec("pm_style_strength", DoubleType, portColor = COLOR_FLOAT,
        description = "--pm-style-strength: PhotoMaker style strength. Default: 20.0."),
    PortSpec("pulid_id_embedding_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--pulid-id-embedding: PuLID id-embedding file path."),
    PortSpec("pulid_id_weight", DoubleType, portColor = COLOR_FLOAT,
        description = "--pulid-id-weight: PuLID id-embedding weight. Default: 1.0."),
    PortSpec("embed_image_metadata", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: false → --disable-image-metadata. Embed generation metadata into output PNG. Default: true. Set false to pass --disable-image-metadata."),
    PortSpec("hires", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.hires config for hires-fix. Null = disabled."),
    PortSpec("cache", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.cache config for inference-step caching. Null = disabled."),
    PortSpec("vae_tiling", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.vae_tiling config. Null = disabled."),
)

private val imageGenOutputs = listOf(
    PortSpec("images", ListType(StringType), portColor = COLOR_IMAGE,
        description = "Paths to the generated image files (one per batch item)."),
)

private val imageGenDefaults = mapOf(
    "negative_prompt"        to WorkflowValue.StringValue(""),
    "width"                  to WorkflowValue.IntValue(-1),
    "height"                 to WorkflowValue.IntValue(-1),
    "seed"                   to WorkflowValue.IntValue(-1),
    "batch_count"            to WorkflowValue.IntValue(1),
    "clip_skip"              to WorkflowValue.IntValue(-1),
    "control_strength"       to WorkflowValue.DoubleValue(0.9),
    "auto_resize_ref_image"  to WorkflowValue.BooleanValue(true),
    "increase_ref_index"     to WorkflowValue.BooleanValue(false),
    "embed_image_metadata"   to WorkflowValue.BooleanValue(true),
    "pm_style_strength"      to WorkflowValue.DoubleValue(20.0),
    "pulid_id_weight"        to WorkflowValue.DoubleValue(1.0),
)

/** Node specs for text-to-image and image-to-image generation. */
object SdImageSpecs {
    /** Text-to-image: `sd_img_gen_params_t` with `init_image` absent. */
    val txt2img = NodeSpec(
        type = "sd.txt2img",
        label = "SD Text→Image",
        description = "Generates images from text using stable-diffusion.cpp (sd --mode img_gen, no init image).",
        category = CATEGORY_SD,
        inputs = imageGenSharedPorts,
        outputs = imageGenOutputs,
        defaultValues = imageGenDefaults,
    )

    /** Image-to-image: `sd_img_gen_params_t` with `init_image` set. */
    val img2img = NodeSpec(
        type = "sd.img2img",
        label = "SD Image→Image",
        description = "Re-generates an image from an existing one using stable-diffusion.cpp (sd --mode img_gen --init-img).",
        category = CATEGORY_SD,
        inputs = imageGenSharedPorts + listOf(
            PortSpec("init_image", StringType, portColor = COLOR_IMAGE,
                description = "--init-img: Path to the initial image for image-to-image generation."),
            PortSpec("strength", DoubleType, portColor = COLOR_FLOAT,
                description = "--strength: Denoising strength — 0.0 preserves input completely, 1.0 ignores it. Default: 0.75."),
        ),
        outputs = imageGenOutputs,
        defaultValues = imageGenDefaults + mapOf(
            "strength" to WorkflowValue.DoubleValue(0.75),
        ),
    )
}
