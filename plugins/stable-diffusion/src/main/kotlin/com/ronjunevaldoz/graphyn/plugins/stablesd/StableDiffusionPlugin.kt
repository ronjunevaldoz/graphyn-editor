package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.plugins.stablesd.SdTokens.vaeTiling
import com.ronjunevaldoz.graphyn.pluginapi.GRAPHYN_PLUGIN_API_VERSION
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPlugin
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginMetadata
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/**
 * Graphyn plugin for [stable-diffusion.cpp](https://github.com/leejet/stable-diffusion.cpp).
 *
 * Registers 10 composable nodes covering the full `sd_ctx_params_t`, `sd_img_gen_params_t`,
 * and `sd_vid_gen_params_t` parameter surfaces:
 *
 * **Config nodes** (reusable across multiple generation nodes):
 * - `sd.context`    — model loading + hardware options
 * - `sd.sampler`    — sampling method, scheduler, CFG, SLG
 * - `sd.lora`       — single LoRA entry (chain N of these)
 * - `sd.hires`      — hires-fix params
 * - `sd.cache`      — inference-step caching
 * - `sd.vae_tiling` — tiled VAE decode for large outputs
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
        registrar.registerNodeSpec(SdContextSpec.context)
        registrar.registerNodeSpec(SdSamplerSpec.sampler)
        registrar.registerNodeSpec(SdLoraSpec.lora)
        registrar.registerNodeSpec(SdHiresSpec.hires)
        registrar.registerNodeSpec(SdCacheSpec.cache)
        registrar.registerNodeSpec(SdTilingSpec.vaeTiling)

        // Generation nodes
        registrar.registerNodeSpec(SdImageSpecs.txt2img)
        registrar.registerNodeSpec(SdImageSpecs.img2img)
        registrar.registerNodeSpec(SdVideoSpecs.txt2vid)
        registrar.registerNodeSpec(SdVideoSpecs.img2vid)

        // Config node executors — wrap inputs as opaque record tokens
        registrar.registerExecutor(SdContextSpec.context.type) { inputs ->
            mapOf("context" to SdTokens.context(inputs))
        }
        registrar.registerExecutor(SdSamplerSpec.sampler.type) { inputs ->
            mapOf("sampler" to SdTokens.sampler(inputs))
        }
        registrar.registerExecutor(SdLoraSpec.lora.type) { inputs ->
            mapOf("lora" to SdTokens.lora(inputs))
        }
        registrar.registerExecutor(SdHiresSpec.hires.type) { inputs ->
            mapOf("hires" to SdTokens.hires(inputs))
        }
        registrar.registerExecutor(SdCacheSpec.cache.type) { inputs ->
            mapOf("cache" to SdTokens.cache(inputs))
        }
        registrar.registerExecutor(SdTilingSpec.vaeTiling.type) { inputs ->
            mapOf("vae_tiling" to SdTokens.vaeTiling(inputs))
        }

        // Generation node executors
        registrar.registerExecutor(SdImageSpecs.txt2img.type, txt2imgExecutor(backend))
        registrar.registerExecutor(SdImageSpecs.img2img.type, img2imgExecutor(backend))
        registrar.registerExecutor(SdVideoSpecs.txt2vid.type, txt2vidExecutor(backend))
        registrar.registerExecutor(SdVideoSpecs.img2vid.type, img2vidExecutor(backend))
    }
}
