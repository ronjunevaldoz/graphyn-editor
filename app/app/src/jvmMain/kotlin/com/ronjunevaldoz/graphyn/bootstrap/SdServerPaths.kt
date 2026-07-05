package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest

/**
 * The distinct server-side file paths an image generation depends on: model/encoder/vae/input
 * paths + LoRAs + reference images. Called *after* [stageLocalImages] so any of these that were
 * local uploads are already server paths — this is a defense-in-depth check (fail loudly here if
 * an upload silently produced a bad path, or if a future image-bearing port is added to the typed
 * request without also being wired into staging) on top of `uploadIfLocal`'s own error handling.
 */
internal fun collectServerPaths(request: SdGenerateImageRequest): List<String> = buildList {
    val ctx = request.context
    listOfNotNull(
        ctx.diffusionModelPath, ctx.highNoiseDiffusionModelPath, ctx.clipLPath, ctx.clipGPath,
        ctx.clipVisionPath, ctx.t5xxlPath, ctx.llmPath, ctx.vaePath,
        request.initImagePath, request.controlNet?.controlImage, request.controlNet?.maskImage,
    ).forEach { add(it) }
    request.idCond?.refImages?.forEach { add(it) }
    request.loras.forEach { add(it.path) }
}.filter { it.isNotBlank() }.distinct()

/** The distinct server-side file paths a video generation depends on. */
internal fun collectServerPaths(request: SdGenerateVideoRequest): List<String> = buildList {
    val ctx = request.context
    listOfNotNull(
        ctx.diffusionModelPath, ctx.highNoiseDiffusionModelPath, ctx.clipVisionPath, ctx.t5xxlPath, ctx.vaePath,
        request.initImagePath,
    ).forEach { add(it) }
    request.loras.forEach { add(it.path) }
}.filter { it.isNotBlank() }.distinct()
