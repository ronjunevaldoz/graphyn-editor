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
    PortSpec("context", OpaqueType, portColor = COLOR_CONTEXT,
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
    PortSpec("controlnet", NullableType(OpaqueType), portColor = COLOR_CONTROLNET,
        description = "Optional sd.controlnet token (ControlNet conditioning + mask). Null = disabled."),
    PortSpec("id_cond", NullableType(OpaqueType), portColor = COLOR_ID_COND,
        description = "Optional sd.id_cond token (ref images, PhotoMaker, PuLID). Null = disabled."),
    PortSpec("embed_image_metadata", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: false → --disable-image-metadata. Embed generation metadata into output PNG. Default: true. Set false to pass --disable-image-metadata."),
    PortSpec("hires", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.hires config for hires-fix. Null = disabled."),
    PortSpec("cache", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.cache config for inference-step caching. Null = disabled."),
    PortSpec("vae_tiling", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.vae_tiling config. Null = disabled."),
    PortSpec("server", NullableType(OpaqueType), portColor = COLOR_SERVER,
        description = "Optional sd.server token — pins this generation to a specific server-sd deployment. Null = use the app-wide server."),
)

private val imageGenOutputs = listOf(
    PortSpec("images", ListType(StringType), portColor = COLOR_IMAGE,
        description = "Paths to the generated image files (one per batch item)."),
    PortSpec("image", NullableType(StringType), portColor = COLOR_IMAGE,
        description = "Path to the first generated image. Convenience for single-image workflows."),
)

private val imageGenDefaults = mapOf(
    "negative_prompt"      to WorkflowValue.StringValue(""),
    "width"                to WorkflowValue.IntValue(-1),
    "height"               to WorkflowValue.IntValue(-1),
    "seed"                 to WorkflowValue.IntValue(-1),
    "batch_count"          to WorkflowValue.IntValue(1),
    "clip_skip"            to WorkflowValue.IntValue(-1),
    "embed_image_metadata" to WorkflowValue.BooleanValue(true),
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
