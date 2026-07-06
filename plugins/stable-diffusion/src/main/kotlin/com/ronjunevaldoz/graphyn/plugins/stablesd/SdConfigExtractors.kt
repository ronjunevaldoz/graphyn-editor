package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.DoubleValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.IntValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.orEmpty

/** Reads a `sd.context` opaque token's fields directly into a typed [SdContextConfig]. */
internal fun extractContextConfig(inputs: Map<String, WorkflowValue>) = SdContextConfig(
    modelPath = inputs.str("model_path"),
    clipLPath = inputs.str("clip_l_path"),
    clipGPath = inputs.str("clip_g_path"),
    clipVisionPath = inputs.str("clip_vision_path"),
    t5xxlPath = inputs.str("t5xxl_path"),
    llmPath = inputs.str("llm_path"),
    llmVisionPath = inputs.str("llm_vision_path"),
    diffusionModelPath = inputs.str("diffusion_model_path"),
    highNoiseDiffusionModelPath = inputs.str("high_noise_diffusion_model_path"),
    uncondDiffusionModelPath = inputs.str("uncond_diffusion_model_path"),
    embeddingsConnectorsPath = inputs.str("embeddings_connectors_path"),
    vaePath = inputs.str("vae_path"),
    vaeFormat = inputs.str("vae_format"),
    audioVaePath = inputs.str("audio_vae_path"),
    taesdPath = inputs.str("taesd_path"),
    esrganPath = inputs.str("esrgan_path"),
    controlNetPath = inputs.str("control_net_path"),
    embeddingDir = inputs.str("embedding_dir"),
    photoMakerPath = inputs.str("photo_maker_path"),
    pulidWeightsPath = inputs.str("pulid_weights_path"),
    loraModelDir = inputs.str("lora_model_dir"),
    hiresUpscalersDir = inputs.str("hires_upscalers_dir"),
    tensorTypeRules = inputs.str("tensor_type_rules"),
    wtype = inputs.str("wtype"),
    nThreads = inputs.int("n_threads"),
    rngType = inputs.str("rng_type"),
    samplerRngType = inputs.str("sampler_rng_type"),
    prediction = inputs.str("prediction"),
    loraApplyMode = inputs.str("lora_apply_mode"),
    offloadParamsToCpu = inputs.bool("offload_params_to_cpu") == true,
    maxVram = inputs.str("max_vram"),
    backend = inputs.str("backend"),
    paramsBackend = inputs.str("params_backend"),
    rpcServers = inputs.str("rpc_servers"),
    enableMmap = inputs.bool("enable_mmap") == true,
    flashAttn = inputs.bool("flash_attn") == true,
    diffusionFlashAttn = inputs.bool("diffusion_flash_attn") == true,
    diffusionConvDirect = inputs.bool("diffusion_conv_direct") == true,
    vaeConvDirect = inputs.bool("vae_conv_direct") == true,
    clipOnCpu = inputs.bool("clip_on_cpu") == true,
    vaeOnCpu = inputs.bool("vae_on_cpu") == true,
    controlNetCpu = inputs.bool("control_net_cpu") == true,
    streamLayers = inputs.bool("stream_layers") == true,
    eagerLoad = inputs.bool("eager_load") == true,
    circular = inputs.bool("circular") == true,
    circularX = inputs.bool("circular_x") == true,
    circularY = inputs.bool("circular_y") == true,
    forceSdxlVaeConvScale = inputs.bool("force_sdxl_vae_conv_scale") == true,
    chromaUseDitMask = inputs.bool("chroma_use_dit_mask"),
    chromaUseT5Mask = inputs.bool("chroma_use_t5_mask") == true,
    chromaT5MaskPad = inputs.int("chroma_t5_mask_pad"),
    qwenImageZeroCondT = inputs.bool("qwen_image_zero_cond_t") == true,
)

/** Reads a `sd.sampler` opaque token's fields directly into a typed [SdSamplerConfig]. */
internal fun extractSamplerConfig(inputs: Map<String, WorkflowValue>) = SdSamplerConfig(
    sampleMethod = inputs.str("sample_method"),
    scheduler = inputs.str("scheduler"),
    sampleSteps = inputs.int("sample_steps"),
    txtCfg = inputs.double("txt_cfg"),
    imgCfg = inputs.double("img_cfg"),
    distilledGuidance = inputs.double("distilled_guidance"),
    eta = inputs.double("eta"),
    flowShift = inputs.double("flow_shift"),
    shiftedTimestep = inputs.int("shifted_timestep"),
    customSigmas = inputs["custom_sigmas"]?.asList()?.map { (it as? DoubleValue)?.value ?: 0.0 },
    extraSampleArgs = inputs.str("extra_sample_args"),
    slgLayers = inputs["slg_layers"]?.asList()?.map { (it as? IntValue)?.value ?: 0 },
    slgLayerStart = inputs.double("slg_layer_start"),
    slgLayerEnd = inputs.double("slg_layer_end"),
    slgScale = inputs.double("slg_scale"),
)

/** Reads a `sd.hires` opaque token's fields into [SdHiresConfig], or null when disabled/absent. */
internal fun extractHiresConfig(token: WorkflowValue?): SdHiresConfig? {
    val inputs = token.orEmpty()
    if (inputs.bool("enabled") != true) return null
    return SdHiresConfig(
        enabled = true,
        upscaler = inputs.str("upscaler"),
        modelPath = inputs.str("model_path"),
        scale = inputs.double("scale"),
        targetWidth = inputs.int("target_width"),
        targetHeight = inputs.int("target_height"),
        steps = inputs.int("steps"),
        denoisingStrength = inputs.double("denoising_strength"),
        upscaleTileSize = inputs.int("upscale_tile_size"),
        customSigmas = inputs["custom_sigmas"]?.asList()?.map { (it as? DoubleValue)?.value ?: 0.0 },
    )
}

/** Reads a `sd.cache` opaque token's fields into [SdCacheConfig], or null when disabled/absent. */
internal fun extractCacheConfig(token: WorkflowValue?): SdCacheConfig? {
    val inputs = token.orEmpty()
    val mode = inputs.str("mode") ?: return null
    if (mode == "disabled") return null
    return SdCacheConfig(
        mode = mode,
        reuseThreshold = inputs.double("reuse_threshold"),
        startPercent = inputs.double("start_percent"),
        endPercent = inputs.double("end_percent"),
        errorDecayRate = inputs.double("error_decay_rate"),
        useRelativeThreshold = inputs.bool("use_relative_threshold"),
        resetErrorOnCompute = inputs.bool("reset_error_on_compute"),
        fnComputeBlocks = inputs.int("fn_compute_blocks"),
        bnComputeBlocks = inputs.int("bn_compute_blocks"),
        maxWarmupSteps = inputs.int("max_warmup_steps"),
        spectrumW = inputs.double("spectrum_w"),
        spectrumM = inputs.int("spectrum_m"),
        spectrumLam = inputs.double("spectrum_lam"),
        spectrumWindowSize = inputs.int("spectrum_window_size"),
        spectrumFlexWindow = inputs.double("spectrum_flex_window"),
        spectrumWarmupSteps = inputs.int("spectrum_warmup_steps"),
        spectrumStopPercent = inputs.double("spectrum_stop_percent"),
        scmMask = inputs.str("scm_mask"),
        scmPolicyDynamic = inputs.bool("scm_policy_dynamic"),
    )
}

/** Reads a `sd.vae_tiling` opaque token's fields into [SdTilingConfig], or null when disabled/absent. */
internal fun extractTilingConfig(token: WorkflowValue?): SdTilingConfig? {
    val inputs = token.orEmpty()
    if (inputs.bool("enabled") != true) return null
    return SdTilingConfig(
        enabled = true,
        temporalTiling = inputs.bool("temporal_tiling") == true,
        tileSizeX = inputs.int("tile_size_x"),
        targetOverlap = inputs.double("target_overlap"),
        relSizeX = inputs.double("rel_size_x"),
        extraTilingArgs = inputs.str("extra_tiling_args"),
    )
}

/** Reads a `sd.controlnet` opaque token's fields into [SdControlNetConfig], or null when absent. */
internal fun extractControlNetConfig(token: WorkflowValue?): SdControlNetConfig? {
    if (token == null || token is WorkflowValue.NullValue) return null
    val inputs = token.orEmpty()
    return SdControlNetConfig(
        controlImage = inputs.str("control_image"),
        controlStrength = inputs.double("control_strength"),
        maskImage = inputs.str("mask_image"),
    )
}

/** Reads a `sd.id_cond` opaque token's fields into [SdIdCondConfig], or null when absent. */
internal fun extractIdCondConfig(token: WorkflowValue?): SdIdCondConfig? {
    if (token == null || token is WorkflowValue.NullValue) return null
    val inputs = token.orEmpty()
    return SdIdCondConfig(
        refImages = (inputs["ref_images"] as? WorkflowValue.ListValue)?.items
            ?.filterIsInstance<StringValue>()?.map { it.value }.orEmpty(),
        autoResizeRefImage = inputs.bool("auto_resize_ref_image"),
        increaseRefIndex = inputs.bool("increase_ref_index") == true,
        pmIdEmbedPath = inputs.str("pm_id_embed_path"),
        pmIdImagesDir = inputs.str("pm_id_images_dir"),
        pmStyleStrength = inputs.double("pm_style_strength"),
        pulidIdEmbeddingPath = inputs.str("pulid_id_embedding_path"),
        pulidIdWeight = inputs.double("pulid_id_weight"),
    )
}

/** Reads a `sd.server` opaque token's fields into [SdServerConfig], or null when absent. */
internal fun extractServerConfig(token: WorkflowValue?): SdServerConfig? {
    if (token == null || token is WorkflowValue.NullValue) return null
    val inputs = token.orEmpty()
    return SdServerConfig(
        baseUrl = inputs.str("base_url"),
        apiKey = inputs.str("api_key"),
    )
}

/** Reads the `loras` list input (list of `sd.lora` opaque tokens) into [SdLoraConfig]s. */
internal fun extractLoras(inputs: Map<String, WorkflowValue>): List<SdLoraConfig> =
    (inputs["loras"] as? WorkflowValue.ListValue)?.items?.mapNotNull { loraToken ->
        val lora = loraToken.orEmpty()
        val path = (lora["path"] as? StringValue)?.value ?: return@mapNotNull null
        val mult = when (val m = lora["multiplier"]) {
            is DoubleValue -> m.value
            is IntValue -> m.value.toDouble()
            else -> 1.0
        }
        SdLoraConfig(path, mult)
    }.orEmpty()
