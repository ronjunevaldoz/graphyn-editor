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
 * The default implementation ([SdCliBackend]) shells out to the `sd-cli` binary.
 * Swap with a JNI implementation to avoid process-per-generation overhead.
 */
interface StableDiffusionBackend {
    /**
     * Generate images using the CLI `--mode img_gen` path.
     *
     * [contextArgs] and [genArgs] are the already-assembled CLI flag lists from [SdCliArgs].
     * The implementation is responsible for invoking the binary and collecting output paths.
     */
    fun generateImage(args: List<String>): SdImageResult

    /**
     * Generate video frames using the CLI `--mode vid_gen` path.
     *
     * Returns frame paths in display order and an optional audio file path.
     */
    fun generateVideo(args: List<String>): SdVideoResult
}
