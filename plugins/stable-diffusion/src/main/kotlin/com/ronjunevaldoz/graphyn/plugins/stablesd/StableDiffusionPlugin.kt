package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * Graphyn plugin for [stable-diffusion.cpp](https://github.com/leejet/stable-diffusion.cpp).
 *
 * Registers 17 composable nodes covering the full `sd_ctx_params_t`, `sd_img_gen_params_t`,
 * and `sd_vid_gen_params_t` parameter surfaces:
 *
 * **Config nodes** (reusable across multiple generation nodes):
 * - `sd.diffusion`  — diffusion model weights (UNet/DiT checkpoint or split-format paths)
 * - `sd.model`      — assembles model token from sd.diffusion, sd.encoders, sd.vae + aux paths
 * - `sd.encoders`   — text encoder paths (CLIP-L, CLIP-G, T5-XXL, LLM, etc.)
 * - `sd.vae`        — VAE decoder path and format override
 * - `sd.context`    — hardware/compute options; takes sd.model token as input
 * - `sd.controlnet` — ControlNet conditioning image + inpainting mask (optional)
 * - `sd.id_cond`    — reference images, PhotoMaker, and PuLID id-conditioning (optional)
 * - `sd.sampler`    — sampling method, scheduler, CFG, SLG
 * - `sd.lora`       — single LoRA entry (chain N of these)
 * - `sd.hires`      — hires-fix params
 * - `sd.cache`      — inference-step caching
 * - `sd.vae_tiling` — tiled VAE decode for large outputs
 * - `sd.server`     — remote server-sd deployment (URL + API key); wire per-generation to support
 *   multiple servers (e.g. a local box and a Modal-hosted instance) in one workflow
 *
 * **Generation nodes**:
 * - `sd.txt2img` — text → image(s)
 * - `sd.img2img` — image+text → image(s)
 * - `sd.txt2vid` — text → video frames
 * - `sd.img2vid` — image+text → video frames
 *
 * @param backend Backend implementation. Defaults to [SdCliBackend] using the binary at [cliPath].
 * @param cliPath Path to the `sd-cli` executable from stable-diffusion.cpp.
 * @param outputDir Directory where generated files are stored.
 */
class StableDiffusionPlugin(
    private val backend: StableDiffusionBackend = SdCliBackend(
        cliPath = System.getenv("SD_CLI_PATH") ?: "sd-cli",
    ),
) : GraphynPlugin {

    override val metadata = GraphynPluginMetadata(
        id = "graphyn.stable-diffusion",
        displayName = "Stable Diffusion (sd.cpp)",
        version = "1.0.0",
        apiVersion = GRAPHYN_PLUGIN_API_VERSION,
    )

    override fun register(registrar: GraphynPluginRegistrar) {
        // Config nodes
        registrar.registerNodeSpec(SdDiffusionSpec.diffusion)
        registrar.registerNodeSpec(SdModelSpec.model)
        registrar.registerNodeSpec(SdEncodersSpec.encoders)
        registrar.registerNodeSpec(SdVaeSpec.vae)
        registrar.registerNodeSpec(SdContextSpec.context)
        registrar.registerNodeSpec(SdSeamlessSpec.seamless)
        registrar.registerNodeSpec(SdChromaSpec.chroma)
        registrar.registerNodeSpec(SdOffloadSpec.offload)
        registrar.registerNodeSpec(SdSamplerSpec.sampler)
        registrar.registerNodeSpec(SdLoraSpec.lora)
        registrar.registerNodeSpec(SdHiresSpec.hires)
        registrar.registerNodeSpec(SdCacheSpec.cache)
        registrar.registerNodeSpec(SdTilingSpec.vaeTiling)
        registrar.registerNodeSpec(SdControlNetSpec.controlNet)
        registrar.registerNodeSpec(SdIdCondSpec.idCond)
        registrar.registerNodeSpec(SdServerSpec.server)

        // Generation nodes
        registrar.registerNodeSpec(SdImageSpecs.txt2img)
        registrar.registerNodeSpec(SdImageSpecs.img2img)
        registrar.registerNodeSpec(SdVideoSpecs.txt2vid)
        registrar.registerNodeSpec(SdVideoSpecs.img2vid)

        // Config node executors — wrap inputs as opaque record tokens
        registerConfigNodeExecutors(registrar)

        // Generation node executors
        registrar.registerExecutor(SdImageSpecs.txt2img.type, txt2imgExecutor(backend))
        registrar.registerExecutor(SdImageSpecs.img2img.type, img2imgExecutor(backend))
        registrar.registerExecutor(SdVideoSpecs.txt2vid.type, txt2vidExecutor(backend))
        registrar.registerExecutor(SdVideoSpecs.img2vid.type, img2vidExecutor(backend))
    }
}
