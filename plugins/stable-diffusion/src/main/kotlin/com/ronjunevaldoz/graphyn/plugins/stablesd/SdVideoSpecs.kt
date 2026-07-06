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

/** Shared input ports for both video generation nodes (from sd_vid_gen_params_t). */
private val videoGenSharedPorts = listOf(
    PortSpec("context", OpaqueType, portColor = COLOR_CONTEXT,
        description = "SD model context from sd.context node."),
    PortSpec("sampler", OpaqueType, portColor = COLOR_SAMPLER,
        description = "Primary sampling config from sd.sampler node."),
    PortSpec("high_noise_sampler", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.sampler for the high-noise pass of MoE video models (Wan2.1/Wan2.2). Null = reuse sampler."),
    PortSpec("prompt", StringType, portColor = COLOR_STRING,
        description = "--prompt: Positive text conditioning."),
    PortSpec("negative_prompt", StringType, portColor = COLOR_STRING,
        description = "--negative-prompt: Negative conditioning text."),
    PortSpec("width", IntType, portColor = COLOR_INT,
        description = "--width: Frame width in pixels. -1 = model default."),
    PortSpec("height", IntType, portColor = COLOR_INT,
        description = "--height: Frame height in pixels. -1 = model default."),
    PortSpec("seed", IntType, portColor = COLOR_INT,
        description = "--seed: RNG seed. -1 = random."),
    PortSpec("clip_skip", IntType, portColor = COLOR_INT,
        description = "--clip-skip: CLIP layers to skip from the end. -1 = model default."),
    PortSpec("video_frames", IntType, portColor = COLOR_INT,
        description = "--video-frames: Number of frames to generate. Default: 1."),
    PortSpec("fps", IntType, portColor = COLOR_INT,
        description = "--fps: Output frame rate hint (stored in metadata/container). Default: 16."),
    PortSpec("moe_boundary", DoubleType, portColor = COLOR_FLOAT,
        description = "--moe-boundary: Denoising fraction at which MoE models switch from high-noise to low-noise model. Default: 0.875."),
    PortSpec("vace_strength", DoubleType, portColor = COLOR_FLOAT,
        description = "--vace-strength: VACE conditioning strength for Wan2.1 Vace models. Default: 1.0."),
    PortSpec("loras", NullableType(ListType(OpaqueType)), portColor = COLOR_LORA,
        description = "List of sd.lora tokens to apply. Null = none."),
    PortSpec("control_frames", NullableType(ListType(StringType)), portColor = COLOR_IMAGE,
        description = "--control-video: Paths to per-frame control images. Null = none."),
    PortSpec("embed_image_metadata", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: false → --disable-image-metadata. Embed generation metadata into output frames. Default: true. Set false to pass --disable-image-metadata."),
    PortSpec("hires", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.hires config. Null = disabled."),
    PortSpec("cache", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.cache config. Null = disabled."),
    PortSpec("vae_tiling", NullableType(OpaqueType), portColor = COLOR_SAMPLER,
        description = "Optional sd.vae_tiling config. Null = disabled."),
    PortSpec("server", NullableType(OpaqueType), portColor = COLOR_SERVER,
        description = "Optional sd.server token — pins this generation to a specific server-sd deployment. Null = use the app-wide server."),
)

private val videoGenOutputs = listOf(
    PortSpec("frames", ListType(StringType), portColor = COLOR_VIDEO,
        description = "Paths to the output frame image files in display order."),
    PortSpec("audio_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "Path to the generated audio file, if the model produces audio. Null otherwise."),
)

private val videoGenDefaults = mapOf(
    "negative_prompt" to WorkflowValue.StringValue(""),
    "width"           to WorkflowValue.IntValue(-1),
    "height"          to WorkflowValue.IntValue(-1),
    "seed"            to WorkflowValue.IntValue(-1),
    "clip_skip"       to WorkflowValue.IntValue(-1),
    "video_frames"    to WorkflowValue.IntValue(1),
    "fps"             to WorkflowValue.IntValue(16),
    "moe_boundary"         to WorkflowValue.DoubleValue(0.875),
    "vace_strength"        to WorkflowValue.DoubleValue(1.0),
    "embed_image_metadata" to WorkflowValue.BooleanValue(true),
)

/** Node specs for text-to-video and image-to-video generation. */
object SdVideoSpecs {
    /** Text-to-video: `sd_vid_gen_params_t` with `init_image` absent. */
    val txt2vid = NodeSpec(
        type = "sd.txt2vid",
        label = "SD Text→Video",
        description = "Generates video frames from text using stable-diffusion.cpp (sd --mode vid_gen, no init image).",
        category = CATEGORY_SD,
        inputs = videoGenSharedPorts,
        outputs = videoGenOutputs,
        defaultValues = videoGenDefaults,
    )

    /** Image-to-video: `sd_vid_gen_params_t` with `init_image` set. */
    val img2vid = NodeSpec(
        type = "sd.img2vid",
        label = "SD Image→Video",
        description = "Generates video frames from an initial image using stable-diffusion.cpp (sd --mode vid_gen --init-img).",
        category = CATEGORY_SD,
        inputs = videoGenSharedPorts + listOf(
            PortSpec("init_image", StringType, portColor = COLOR_IMAGE,
                description = "--init-img: Path to the initial frame / anchor image."),
            PortSpec("end_image", NullableType(StringType), portColor = COLOR_IMAGE,
                description = "--end-img: Path to the target end-frame for interpolation models. Null = not used."),
            PortSpec("strength", DoubleType, portColor = COLOR_FLOAT,
                description = "--strength: Denoising strength applied to the init image. Default: 0.75."),
        ),
        outputs = videoGenOutputs,
        defaultValues = videoGenDefaults + mapOf(
            "strength" to WorkflowValue.DoubleValue(0.75),
        ),
    )
}
