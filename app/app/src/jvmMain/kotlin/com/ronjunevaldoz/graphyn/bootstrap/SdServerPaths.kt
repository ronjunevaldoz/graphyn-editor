package com.ronjunevaldoz.graphyn.bootstrap

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateImageRequest
import com.ronjunevaldoz.graphyn.plugins.stablesd.SdGenerateVideoRequest

/** The distinct server-side file paths an image generation depends on: model/encoder/vae/input paths + LoRAs. */
internal fun collectServerPaths(request: SdGenerateImageRequest): List<String> = buildList {
    val ctx = request.context
    listOfNotNull(
        ctx.diffusionModelPath, ctx.highNoiseDiffusionModelPath, ctx.clipLPath, ctx.clipGPath,
        ctx.clipVisionPath, ctx.t5xxlPath, ctx.llmPath, ctx.vaePath,
        request.initImagePath, request.controlNet?.controlImage, request.controlNet?.maskImage,
    ).forEach { add(it) }
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
