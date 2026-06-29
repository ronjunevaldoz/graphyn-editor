package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/** Builds `sd-cli` args from an `sd.context` record (sd_ctx_params_t). */
internal fun buildContextArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    inputs.str("model_path")?.let { add("--model"); add(it) }
    inputs.str("clip_l_path")?.let { add("--clip_l"); add(it) }
    inputs.str("clip_g_path")?.let { add("--clip_g"); add(it) }
    inputs.str("clip_vision_path")?.let { add("--clip_vision"); add(it) }
    inputs.str("t5xxl_path")?.let { add("--t5xxl"); add(it) }
    inputs.str("llm_path")?.let { add("--llm"); add(it) }
    inputs.str("llm_vision_path")?.let { add("--llm_vision"); add(it) }
    inputs.str("diffusion_model_path")?.let { add("--diffusion-model"); add(it) }
    inputs.str("high_noise_diffusion_model_path")?.let { add("--high-noise-diffusion-model"); add(it) }
    inputs.str("uncond_diffusion_model_path")?.let { add("--uncond-diffusion-model"); add(it) }
    inputs.str("embeddings_connectors_path")?.let { add("--embeddings-connectors"); add(it) }
    inputs.str("vae_path")?.let { add("--vae"); add(it) }
    inputs.str("vae_format")?.let { add("--vae-format"); add(it) }
    inputs.str("audio_vae_path")?.let { add("--audio-vae"); add(it) }
    inputs.str("taesd_path")?.let { add("--taesd"); add(it) }
    inputs.str("esrgan_path")?.let { add("--upscale-model"); add(it) }
    inputs.str("control_net_path")?.let { add("--control-net"); add(it) }
    inputs.str("embedding_dir")?.let { add("--embd-dir"); add(it) }
    inputs.str("photo_maker_path")?.let { add("--photo-maker"); add(it) }
    inputs.str("pulid_weights_path")?.let { add("--pulid-weights"); add(it) }
    inputs.str("lora_model_dir")?.let { add("--lora-model-dir"); add(it) }
    inputs.str("hires_upscalers_dir")?.let { add("--hires-upscalers-dir"); add(it) }
    inputs.str("tensor_type_rules")?.let { add("--tensor-type-rules"); add(it) }
    inputs.str("wtype")?.let { add("--type"); add(it) }
    inputs.int("n_threads")?.let { add("--threads"); add(it.toString()) }
    inputs.str("rng_type")?.let { add("--rng"); add(it) }
    inputs.str("sampler_rng_type")?.let { add("--sampler-rng"); add(it) }
    inputs.str("prediction")?.let { add("--prediction"); add(it) }
    inputs.str("lora_apply_mode")?.let { add("--lora-apply-mode"); add(it) }
    if (inputs.bool("offload_params_to_cpu") == true) add("--offload-to-cpu")
    inputs.str("max_vram")?.let { add("--max-vram"); add(it) }
    inputs.str("backend")?.let { add("--backend"); add(it) }
    inputs.str("params_backend")?.let { add("--params-backend"); add(it) }
    inputs.str("rpc_servers")?.let { add("--rpc-servers"); add(it) }
    if (inputs.bool("enable_mmap") == true) add("--mmap")
    if (inputs.bool("flash_attn") == true) add("--fa")
    if (inputs.bool("diffusion_flash_attn") == true) add("--diffusion-fa")
    if (inputs.bool("diffusion_conv_direct") == true) add("--diffusion-conv-direct")
    if (inputs.bool("vae_conv_direct") == true) add("--vae-conv-direct")
    if (inputs.bool("clip_on_cpu") == true) add("--clip-on-cpu")
    if (inputs.bool("vae_on_cpu") == true) add("--vae-on-cpu")
    if (inputs.bool("control_net_cpu") == true) add("--control-net-cpu")
    if (inputs.bool("stream_layers") == true) add("--stream-layers")
    if (inputs.bool("eager_load") == true) add("--eager-load")
    if (inputs.bool("circular") == true) add("--circular")
    if (inputs.bool("circular_x") == true) add("--circularx")
    if (inputs.bool("circular_y") == true) add("--circulary")
    if (inputs.bool("force_sdxl_vae_conv_scale") == true) add("--force-sdxl-vae-conv-scale")
    if (inputs.bool("chroma_use_dit_mask") == false) add("--chroma-disable-dit-mask")
    if (inputs.bool("chroma_use_t5_mask") == true) add("--chroma-enable-t5-mask")
    inputs.int("chroma_t5_mask_pad")?.let { add("--chroma-t5-mask-pad"); add(it.toString()) }
    if (inputs.bool("qwen_image_zero_cond_t") == true) add("--qwen-image-zero-cond-t")
}

/** Builds `--sampling-method` and related sampler args from an `sd.sampler` record. */
internal fun buildSamplerArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    inputs.str("sample_method")?.let { add("--sampling-method"); add(it) }
    inputs.str("scheduler")?.let { add("--scheduler"); add(it) }
    inputs.int("sample_steps")?.let { add("--steps"); add(it.toString()) }
    inputs.double("txt_cfg")?.let { add("--cfg-scale"); add(it.toString()) }
    inputs.double("img_cfg")?.let { add("--img-cfg-scale"); add(it.toString()) }
    inputs.double("distilled_guidance")?.let { add("--guidance"); add(it.toString()) }
    inputs.double("eta")?.let { add("--eta"); add(it.toString()) }
    inputs.double("flow_shift")?.let { add("--flow-shift"); add(it.toString()) }
    inputs.int("shifted_timestep")?.let { add("--timestep-shift"); add(it.toString()) }
    inputs["custom_sigmas"]?.asList()?.let { s ->
        add("--sigmas"); add(s.joinToString(",") { (it as? WorkflowValue.DoubleValue)?.value?.toString() ?: it.toString() })
    }
    inputs.str("extra_sample_args")?.let { add("--extra-sample-args"); add(it) }
    inputs["slg_layers"]?.asList()?.let { layers ->
        add("--skip-layers"); add(layers.joinToString(",") { (it as? WorkflowValue.IntValue)?.value?.toString() ?: it.toString() })
    }
    inputs.double("slg_layer_start")?.let { add("--skip-layer-start"); add(it.toString()) }
    inputs.double("slg_layer_end")?.let { add("--skip-layer-end"); add(it.toString()) }
    inputs.double("slg_scale")?.let { add("--slg-scale"); add(it.toString()) }
}

/** High-noise sampler flags for MoE video models (Wan2.1/Wan2.2). Only emits flags with a `--high-noise-*` CLI equivalent. */
internal fun buildHighNoiseSamplerArgs(inputs: Map<String, WorkflowValue>): List<String> = buildList {
    inputs.str("sample_method")?.let { add("--high-noise-sampling-method"); add(it) }
    inputs.int("sample_steps")?.let { add("--high-noise-steps"); add(it.toString()) }
    inputs.double("txt_cfg")?.let { add("--high-noise-cfg-scale"); add(it.toString()) }
    inputs.double("img_cfg")?.let { add("--high-noise-img-cfg-scale"); add(it.toString()) }
    inputs.double("distilled_guidance")?.let { add("--high-noise-guidance"); add(it.toString()) }
    inputs.double("eta")?.let { add("--high-noise-eta"); add(it.toString()) }
    inputs["slg_layers"]?.asList()?.let { layers ->
        add("--high-noise-skip-layers"); add(layers.joinToString(",") { (it as? WorkflowValue.IntValue)?.value?.toString() ?: it.toString() })
    }
    inputs.double("slg_layer_start")?.let { add("--high-noise-skip-layer-start"); add(it.toString()) }
    inputs.double("slg_layer_end")?.let { add("--high-noise-skip-layer-end"); add(it.toString()) }
    inputs.double("slg_scale")?.let { add("--high-noise-slg-scale"); add(it.toString()) }
}
