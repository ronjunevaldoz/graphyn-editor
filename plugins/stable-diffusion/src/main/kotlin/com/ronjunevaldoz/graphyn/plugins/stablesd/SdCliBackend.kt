package com.ronjunevaldoz.graphyn.plugins.stablesd

import java.io.File

/**
 * [StableDiffusionBackend] implementation that shells out to the `sd-cli` binary
 * from [stable-diffusion.cpp](https://github.com/leejet/stable-diffusion.cpp).
 *
 * @param cliPath Absolute path to the `sd-cli` executable.
 * @param outputDir Directory where generated files are written. Created if absent.
 */
class SdCliBackend(
    private val cliPath: String,
    private val outputDir: String = System.getProperty("java.io.tmpdir") + "/graphyn-sd",
) : StableDiffusionBackend {

    init { File(outputDir).mkdirs() }

    override fun generateImage(request: SdGenerateImageRequest): SdImageResult {
        val outPrefix = File(outputDir, "img_${System.currentTimeMillis()}").absolutePath
        val fullArgs = listOf(cliPath, "--mode", "img_gen", "--output", outPrefix) + request.toCliArgs()
        runProcess(fullArgs)
        // sd-cli appends _001.png, _002.png … when batch_count > 1; collect all matching files
        val images = File(outputDir).listFiles { f ->
            f.name.startsWith(File(outPrefix).name) && f.extension == "png"
        }?.sorted()?.map { it.absolutePath } ?: emptyList()
        check(images.isNotEmpty()) { "sd-cli produced no output images. Args: $fullArgs" }
        return SdImageResult(images)
    }

    override fun generateVideo(request: SdGenerateVideoRequest): SdVideoResult {
        val outPrefix = File(outputDir, "vid_${System.currentTimeMillis()}").absolutePath
        val fullArgs = listOf(cliPath, "--mode", "vid_gen", "--output", outPrefix) + request.toCliArgs()
        runProcess(fullArgs)
        val frames = File(outputDir).listFiles { f ->
            f.name.startsWith(File(outPrefix).name) && f.extension == "png"
        }?.sorted()?.map { it.absolutePath } ?: emptyList()
        check(frames.isNotEmpty()) { "sd-cli produced no output frames. Args: $fullArgs" }
        val audio = File("$outPrefix.wav").takeIf { it.exists() }?.absolutePath
        return SdVideoResult(frames, audio)
    }

    private fun runProcess(args: List<String>) {
        val proc = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        val log = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()
        check(exit == 0) { "sd-cli exited with code $exit.\nOutput:\n$log" }
    }
}
