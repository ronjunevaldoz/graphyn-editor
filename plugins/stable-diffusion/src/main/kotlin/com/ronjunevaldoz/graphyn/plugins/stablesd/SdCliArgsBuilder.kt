package com.ronjunevaldoz.graphyn.plugins.stablesd

/**
 * Converts a typed [SdGenerateImageRequest]/[SdGenerateVideoRequest] into `--flag value` CLI
 * tokens for [SdCliBackend] — the one place in this plugin that genuinely needs real CLI strings,
 * since it shells out to the actual `sd-cli` binary. Every other consumer (e.g.
 * `HttpStableDiffusionBackend` in app/app) works with the typed request directly.
 */
private fun MutableList<String>.addContextArgs(ctx: SdContextConfig) {
    ctx.modelPath?.let { add("--model"); add(it) }
    ctx.clipLPath?.let { add("--clip_l"); add(it) }
    ctx.clipGPath?.let { add("--clip_g"); add(it) }
    ctx.clipVisionPath?.let { add("--clip_vision"); add(it) }
    ctx.t5xxlPath?.let { add("--t5xxl"); add(it) }
    ctx.llmPath?.let { add("--llm"); add(it) }
    ctx.llmVisionPath?.let { add("--llm_vision"); add(it) }
    ctx.diffusionModelPath?.let { add("--diffusion-model"); add(it) }
    ctx.highNoiseDiffusionModelPath?.let { add("--high-noise-diffusion-model"); add(it) }
    ctx.uncondDiffusionModelPath?.let { add("--uncond-diffusion-model"); add(it) }
    ctx.embeddingsConnectorsPath?.let { add("--embeddings-connectors"); add(it) }
    ctx.vaePath?.let { add("--vae"); add(it) }
    ctx.vaeFormat?.let { add("--vae-format"); add(it) }
    ctx.audioVaePath?.let { add("--audio-vae"); add(it) }
    ctx.taesdPath?.let { add("--taesd"); add(it) }
    ctx.esrganPath?.let { add("--upscale-model"); add(it) }
    ctx.controlNetPath?.let { add("--control-net"); add(it) }
    ctx.embeddingDir?.let { add("--embd-dir"); add(it) }
    ctx.photoMakerPath?.let { add("--photo-maker"); add(it) }
    ctx.pulidWeightsPath?.let { add("--pulid-weights"); add(it) }
    ctx.loraModelDir?.let { add("--lora-model-dir"); add(it) }
    ctx.hiresUpscalersDir?.let { add("--hires-upscalers-dir"); add(it) }
    ctx.tensorTypeRules?.let { add("--tensor-type-rules"); add(it) }
    ctx.wtype?.let { add("--type"); add(it) }
    ctx.nThreads?.let { add("--threads"); add(it.toString()) }
    ctx.rngType?.let { add("--rng"); add(it) }
    ctx.samplerRngType?.let { add("--sampler-rng"); add(it) }
    ctx.prediction?.let { add("--prediction"); add(it) }
    ctx.loraApplyMode?.let { add("--lora-apply-mode"); add(it) }
    if (ctx.offloadParamsToCpu) add("--offload-to-cpu")
    ctx.maxVram?.let { add("--max-vram"); add(it) }
    ctx.backend?.let { add("--backend"); add(it) }
    ctx.paramsBackend?.let { add("--params-backend"); add(it) }
    ctx.rpcServers?.let { add("--rpc-servers"); add(it) }
    if (ctx.enableMmap) add("--mmap")
    if (ctx.flashAttn) add("--fa")
    if (ctx.diffusionFlashAttn) add("--diffusion-fa")
    if (ctx.diffusionConvDirect) add("--diffusion-conv-direct")
    if (ctx.vaeConvDirect) add("--vae-conv-direct")
    if (ctx.clipOnCpu) add("--clip-on-cpu")
    if (ctx.vaeOnCpu) add("--vae-on-cpu")
    if (ctx.controlNetCpu) add("--control-net-cpu")
    if (ctx.streamLayers) add("--stream-layers")
    if (ctx.eagerLoad) add("--eager-load")
    if (ctx.circular) add("--circular")
    if (ctx.circularX) add("--circularx")
    if (ctx.circularY) add("--circulary")
    if (ctx.forceSdxlVaeConvScale) add("--force-sdxl-vae-conv-scale")
    if (ctx.chromaUseDitMask == false) add("--chroma-disable-dit-mask")
    if (ctx.chromaUseT5Mask) add("--chroma-enable-t5-mask")
    ctx.chromaT5MaskPad?.let { add("--chroma-t5-mask-pad"); add(it.toString()) }
    if (ctx.qwenImageZeroCondT) add("--qwen-image-zero-cond-t")
}

private fun MutableList<String>.addSamplerArgs(sampler: SdSamplerConfig, highNoise: Boolean = false) {
    sampler.sampleMethod?.let { add(if (highNoise) "--high-noise-sampling-method" else "--sampling-method"); add(it) }
    if (!highNoise) sampler.scheduler?.let { add("--scheduler"); add(it) }
    sampler.sampleSteps?.let { add(if (highNoise) "--high-noise-steps" else "--steps"); add(it.toString()) }
    sampler.txtCfg?.let { add(if (highNoise) "--high-noise-cfg-scale" else "--cfg-scale"); add(it.toString()) }
    sampler.imgCfg?.let { add(if (highNoise) "--high-noise-img-cfg-scale" else "--img-cfg-scale"); add(it.toString()) }
    sampler.distilledGuidance?.let { add(if (highNoise) "--high-noise-guidance" else "--guidance"); add(it.toString()) }
    sampler.eta?.let { add(if (highNoise) "--high-noise-eta" else "--eta"); add(it.toString()) }
    if (!highNoise) sampler.flowShift?.let { add("--flow-shift"); add(it.toString()) }
    if (!highNoise) sampler.shiftedTimestep?.let { add("--timestep-shift"); add(it.toString()) }
    if (!highNoise) sampler.customSigmas?.let { add("--sigmas"); add(it.joinToString(",")) }
    if (!highNoise) sampler.extraSampleArgs?.let { add("--extra-sample-args"); add(it) }
    sampler.slgLayers?.let { add(if (highNoise) "--high-noise-skip-layers" else "--skip-layers"); add(it.joinToString(",")) }
    sampler.slgLayerStart?.let { add(if (highNoise) "--high-noise-skip-layer-start" else "--skip-layer-start"); add(it.toString()) }
    sampler.slgLayerEnd?.let { add(if (highNoise) "--high-noise-skip-layer-end" else "--skip-layer-end"); add(it.toString()) }
    sampler.slgScale?.let { add(if (highNoise) "--high-noise-slg-scale" else "--slg-scale"); add(it.toString()) }
}

private fun MutableList<String>.addHiresArgs(hires: SdHiresConfig) {
    if (!hires.enabled) return
    add("--hires")
    hires.upscaler?.let { add("--hires-upscaler"); add(it) }
    hires.modelPath?.let { add("--hires-upscalers-dir"); add(it) }
    hires.scale?.let { add("--hires-scale"); add(it.toString()) }
    hires.targetWidth?.takeIf { it > 0 }?.let { add("--hires-width"); add(it.toString()) }
    hires.targetHeight?.takeIf { it > 0 }?.let { add("--hires-height"); add(it.toString()) }
    hires.steps?.takeIf { it > 0 }?.let { add("--hires-steps"); add(it.toString()) }
    hires.denoisingStrength?.let { add("--hires-denoising-strength"); add(it.toString()) }
    hires.upscaleTileSize?.let { add("--hires-upscale-tile-size"); add(it.toString()) }
    hires.customSigmas?.let { add("--hires-sigmas"); add(it.joinToString(",")) }
}

private fun MutableList<String>.addCacheArgs(cache: SdCacheConfig) {
    val mode = cache.mode ?: return
    if (mode == "disabled") return
    add("--cache-mode"); add(mode)
    val opts = buildList {
        cache.reuseThreshold?.let { add("threshold=$it") }
        cache.startPercent?.let { add("start=$it") }
        cache.endPercent?.let { add("end=$it") }
        cache.errorDecayRate?.let { add("decay=$it") }
        cache.useRelativeThreshold?.let { add("relative=$it") }
        cache.resetErrorOnCompute?.let { add("reset=$it") }
        cache.fnComputeBlocks?.let { add("Fn=$it") }
        cache.bnComputeBlocks?.let { add("Bn=$it") }
        cache.maxWarmupSteps?.let { add("warmup=$it") }
        cache.spectrumW?.let { add("w=$it") }
        cache.spectrumM?.let { add("m=$it") }
        cache.spectrumLam?.let { add("lam=$it") }
        cache.spectrumWindowSize?.let { add("window=$it") }
        cache.spectrumFlexWindow?.let { add("flex=$it") }
        cache.spectrumWarmupSteps?.let { add("warmup=$it") }
        cache.spectrumStopPercent?.let { add("stop=$it") }
    }
    if (opts.isNotEmpty()) { add("--cache-option"); add(opts.joinToString(",")) }
    cache.scmMask?.let { add("--scm-mask"); add(it) }
    if (cache.scmPolicyDynamic == false) { add("--scm-policy"); add("static") }
}

private fun MutableList<String>.addTilingArgs(tiling: SdTilingConfig) {
    if (!tiling.enabled) return
    add("--vae-tiling")
    if (tiling.temporalTiling) add("--temporal-tiling")
    tiling.tileSizeX?.takeIf { it > 0 }?.let { add("--vae-tile-size"); add(it.toString()) }
    tiling.targetOverlap?.let { add("--vae-tile-overlap"); add(it.toString()) }
    tiling.relSizeX?.takeIf { it > 0 }?.let { add("--vae-relative-tile-size"); add(it.toString()) }
    tiling.extraTilingArgs?.let { add("--extra-tiling-args"); add(it) }
}

private fun MutableList<String>.addControlNetArgs(controlNet: SdControlNetConfig) {
    controlNet.controlImage?.let { add("--control-image"); add(it) }
    controlNet.controlStrength?.let { add("--control-strength"); add(it.toString()) }
    controlNet.maskImage?.let { add("--mask"); add(it) }
}

private fun MutableList<String>.addIdCondArgs(idCond: SdIdCondConfig) {
    idCond.refImages.forEach { add("--ref-image"); add(it) }
    if (idCond.autoResizeRefImage == false) add("--disable-auto-resize-ref-image")
    if (idCond.increaseRefIndex) add("--increase-ref-index")
    idCond.pmIdEmbedPath?.let { add("--pm-id-embed-path"); add(it) }
    idCond.pmIdImagesDir?.let { add("--pm-id-images-dir"); add(it) }
    idCond.pmStyleStrength?.let { add("--pm-style-strength"); add(it.toString()) }
    idCond.pulidIdEmbeddingPath?.let { add("--pulid-id-embedding"); add(it) }
    idCond.pulidIdWeight?.let { add("--pulid-id-weight"); add(it.toString()) }
}

private fun MutableList<String>.addLoraArgs(loras: List<SdLoraConfig>) {
    loras.forEach { add("--prompt"); add("<lora:${it.path}:${it.multiplier}>") }
}

internal fun SdGenerateImageRequest.toCliArgs(): List<String> = buildList {
    addContextArgs(context)
    addSamplerArgs(sampler)
    hires?.let { addHiresArgs(it) }
    cache?.let { addCacheArgs(it) }
    vaeTiling?.let { addTilingArgs(it) }
    addLoraArgs(loras)

    add("--prompt"); add(prompt)
    if (negativePrompt.isNotBlank()) { add("--negative-prompt"); add(negativePrompt) }
    width?.takeIf { it > 0 }?.let { add("--width"); add(it.toString()) }
    height?.takeIf { it > 0 }?.let { add("--height"); add(it.toString()) }
    add("--seed"); add(seed.toString())
    add("--batch-count"); add(batchCount.toString())
    clipSkip?.takeIf { it > 0 }?.let { add("--clip-skip"); add(it.toString()) }
    initImagePath?.let { add("--init-img"); add(it) }
    strength?.let { add("--strength"); add(it.toString()) }
    if (!embedImageMetadata) add("--disable-image-metadata")

    controlNet?.let { addControlNetArgs(it) }
    idCond?.let { addIdCondArgs(it) }
}

internal fun SdGenerateVideoRequest.toCliArgs(): List<String> = buildList {
    addContextArgs(context)
    addSamplerArgs(sampler)
    highNoiseSampler?.let { addSamplerArgs(it, highNoise = true) }
    hires?.let { addHiresArgs(it) }
    cache?.let { addCacheArgs(it) }
    vaeTiling?.let { addTilingArgs(it) }
    addLoraArgs(loras)

    add("--prompt"); add(prompt)
    if (negativePrompt.isNotBlank()) { add("--negative-prompt"); add(negativePrompt) }
    width?.takeIf { it > 0 }?.let { add("--width"); add(it.toString()) }
    height?.takeIf { it > 0 }?.let { add("--height"); add(it.toString()) }
    add("--seed"); add(seed.toString())
    clipSkip?.takeIf { it > 0 }?.let { add("--clip-skip"); add(it.toString()) }
    videoFrames?.let { add("--video-frames"); add(it.toString()) }
    fps?.let { add("--fps"); add(it.toString()) }
    moeBoundary?.let { add("--moe-boundary"); add(it.toString()) }
    vaceStrength?.let { add("--vace-strength"); add(it.toString()) }
    if (!embedImageMetadata) add("--disable-image-metadata")
    initImagePath?.let { add("--init-img"); add(it) }
    endImagePath?.let { add("--end-img"); add(it) }
    strength?.let { add("--strength"); add(it.toString()) }
    controlFrames.forEach { add("--control-video"); add(it) }
}
