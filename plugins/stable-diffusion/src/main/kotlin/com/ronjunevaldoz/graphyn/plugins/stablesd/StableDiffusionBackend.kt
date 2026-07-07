package com.ronjunevaldoz.graphyn.plugins.stablesd

/** Result from a single image generation call. */
data class SdImageResult(
    val imagePaths: List<String>,
)

/** Result from a video generation call. */
data class SdVideoResult(
    val framePaths: List<String>,
    val audioPath: String? = null,
)

/**
 * Abstraction over a stable-diffusion.cpp backend.
 *
 * The default implementation ([SdCliBackend]) shells out to the `sd-cli` binary, converting
 * [SdGenerateImageRequest]/[SdGenerateVideoRequest] into CLI flags. Swap with a JNI or remote
 * (HTTP) implementation to avoid process-per-generation overhead.
 *
 * Changed 2026-07-08: both methods are `suspend` — callers are workflow executors, which are
 * already suspend (see NodeExecutor), so a plain `fun` here forced HTTP-based implementations
 * to bridge suspend network calls back to blocking via `runBlocking` for no reason.
 */
interface StableDiffusionBackend {
    /** Generate images using the `img_gen` mode. */
    suspend fun generateImage(request: SdGenerateImageRequest): SdImageResult

    /** Generate video frames using the `vid_gen` mode. Returns frame paths and an optional audio path. */
    suspend fun generateVideo(request: SdGenerateVideoRequest): SdVideoResult
}
