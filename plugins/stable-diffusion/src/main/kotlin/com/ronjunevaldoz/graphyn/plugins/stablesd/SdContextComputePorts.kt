package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType

/** Compute/hardware input ports for [SdContextSpec.context], sourced from sd_ctx_params_t. */
internal val sdContextComputePorts: List<PortSpec> = listOf(
    PortSpec("n_threads", IntType, portColor = COLOR_INT,
        description = "CLI: --threads <n>. CPU thread count. -1 = auto-detect physical cores."),
    PortSpec("rng_type", EnumType(SD_RNG_TYPES), portColor = COLOR_STRING,
        description = "CLI: --rng <type>. 'cuda' matches stable-diffusion-webui GPU RNG; 'cpu' matches ComfyUI RNG; 'std_default' uses C++ default."),
    PortSpec("sampler_rng_type", NullableType(EnumType(SD_RNG_TYPES)), portColor = COLOR_STRING,
        description = "CLI: --sampler-rng <type>. Override RNG for the sampler step only. Null = inherit rng_type."),
    PortSpec("prediction", NullableType(EnumType(SD_PREDICTIONS)), portColor = COLOR_STRING,
        description = "CLI: --prediction <type>. Override prediction type: eps (SD1/2), v (v-prediction), edm_v (EDM), sd3_flow (SD3), flux_flow (FLUX), sefi_flow. Null = auto-detect from model."),
    PortSpec("lora_apply_mode", EnumType(SD_LORA_MODES), portColor = COLOR_STRING,
        description = "CLI: --lora-apply-mode <mode>. When LoRA weights are fused: 'auto' = decide at load time, 'immediately' = fuse at load, 'at_runtime' = fuse per-step."),
    PortSpec("offload_params_to_cpu", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --offload-to-cpu. Offload model parameters to CPU RAM between steps to reduce VRAM usage. Increases generation time."),
    PortSpec("max_vram", NullableType(DoubleType), portColor = COLOR_FLOAT,
        description = "CLI: --max-vram <GiB>. VRAM budget in GiB for graph-cut offload. 0.0 = disabled, -1.0 = auto."),
    PortSpec("backend", NullableType(EnumType(SD_BACKENDS)), portColor = COLOR_STRING,
        description = "CLI: --backend <name>. Compute backend. Null = auto-select."),
    PortSpec("params_backend", NullableType(EnumType(SD_BACKENDS)), portColor = COLOR_STRING,
        description = "CLI: --params-backend <name>. Backend for parameter storage. Null = same as backend."),
    PortSpec("rpc_servers", NullableType(StringType), portColor = COLOR_STRING,
        description = "CLI: --rpc-servers <host:port,...>. Comma-separated RPC server addresses for distributed multi-GPU inference."),
    PortSpec("enable_mmap", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --mmap. Use memory-mapped I/O for model loading. Reduces RAM use but may be slower on first load."),
    PortSpec("flash_attn", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --fa. Enable flash attention on all attention layers to reduce VRAM and often increase speed."),
    PortSpec("diffusion_flash_attn", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --diffusion-fa. Enable flash attention only on the diffusion model (not CLIP/VAE). Finer-grained than --fa."),
    PortSpec("diffusion_conv_direct", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --diffusion-conv-direct. Use direct (non-GEMM) convolution on the diffusion model. May be faster on some hardware."),
    PortSpec("vae_conv_direct", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --vae-conv-direct. Use direct convolution on the VAE encoder/decoder."),
    PortSpec("clip_on_cpu", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --clip-on-cpu. Run CLIP text encoder(s) on CPU. Frees GPU VRAM at the cost of slower text encoding."),
    PortSpec("vae_on_cpu", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --vae-on-cpu. Run VAE encoder/decoder on CPU. Frees GPU VRAM at the cost of slower encode/decode."),
    PortSpec("control_net_cpu", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --control-net-cpu. Run ControlNet on CPU (SD1.5 ControlNet only). Frees GPU VRAM during control pass."),
    PortSpec("stream_layers", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --stream-layers. Enable layer residency + prefetch streaming on top of --max-vram. Has no effect without max_vram set."),
    PortSpec("eager_load", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --eager-load. Load all parameters into the params backend at model-load time instead of lazily on first use."),
    // circular / circular_x / circular_y moved to the sd.seamless sub-node (wired to the seamless port).
    PortSpec("force_sdxl_vae_conv_scale", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --force-sdxl-vae-conv-scale. Force the SDXL VAE conv scale factor. Fixes VAE decode artefacts on some merged SDXL checkpoints."),
    PortSpec("chroma_use_dit_mask", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: false → --chroma-disable-dit-mask. Controls Chroma model DiT mask. Default true (enabled). Set false to pass --chroma-disable-dit-mask."),
    PortSpec("chroma_use_t5_mask", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: true → --chroma-enable-t5-mask. Controls Chroma model T5 mask. Default false (disabled). Set true to pass --chroma-enable-t5-mask."),
    PortSpec("chroma_t5_mask_pad", IntType, portColor = COLOR_INT,
        description = "CLI: --chroma-t5-mask-pad <n>. Padding size for the Chroma T5 mask. Default: 1."),
    PortSpec("qwen_image_zero_cond_t", BooleanType, portColor = COLOR_BOOL,
        description = "CLI: --qwen-image-zero-cond-t. Use a zero conditioning timestep for Qwen image models. Only relevant for Qwen-Image / Qwen-Image-Edit series."),
)
