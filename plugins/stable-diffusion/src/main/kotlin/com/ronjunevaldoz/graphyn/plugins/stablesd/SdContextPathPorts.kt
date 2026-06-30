package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType

/**
 * Auxiliary model-path ports for [SdModelSpec.model] — paths that aren't diffusion weights,
 * encoders, or VAE.
 *
 * Diffusion model paths live in [SdDiffusionSpec].
 * Encoder paths (clip_l, t5xxl, llm, …) live in [SdEncodersSpec].
 * VAE paths (vae_path, taesd, …) live in [SdVaeSpec].
 */
internal val sdContextPathPorts: List<PortSpec> = listOf(
    PortSpec("esrgan_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--upscale-model: ESRGAN upscaler model path (used in upscale mode)."),
    PortSpec("control_net_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--control-net: ControlNet model path (SD1.5 only; for other variants use sd.controlnet)."),
    PortSpec("embedding_dir", NullableType(StringType), portColor = COLOR_STRING,
        description = "--embd-dir: Directory of textual inversion embeddings; names usable in the prompt."),
    PortSpec("photo_maker_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--photo-maker: PhotoMaker weights path."),
    PortSpec("pulid_weights_path", NullableType(StringType), portColor = COLOR_STRING,
        description = "--pulid-weights: PuLID weights path."),
    PortSpec("lora_model_dir", NullableType(StringType), portColor = COLOR_STRING,
        description = "--lora-model-dir: Directory searched for LoRA .safetensors files. Default: current dir."),
    PortSpec("hires_upscalers_dir", NullableType(StringType), portColor = COLOR_STRING,
        description = "--hires-upscalers-dir: Directory for hires fix upscaler model files."),
    PortSpec("tensor_type_rules", NullableType(StringType), portColor = COLOR_STRING,
        description = "--tensor-type-rules: Fine-grained per-tensor quantization rules string."),
    PortSpec("wtype", NullableType(EnumType(SD_WEIGHT_TYPES)), portColor = COLOR_STRING,
        description = "--type: Weight quantization type applied at load (f16, bf16, q4_k, q8_0, etc.). Default: original."),
)
