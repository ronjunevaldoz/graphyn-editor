package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.pluginapi.GraphynPluginRegistrar

/** Registers executors for every `sd.*` config node — each just wraps its inputs as an opaque record token. */
internal fun registerConfigNodeExecutors(registrar: GraphynPluginRegistrar) {
    registrar.registerExecutor(SdDiffusionSpec.diffusion.type) { inputs ->
        mapOf("diffusion" to SdTokens.diffusion(inputs))
    }
    registrar.registerExecutor(SdEncodersSpec.encoders.type) { inputs ->
        mapOf("encoders" to SdTokens.encoders(inputs))
    }
    registrar.registerExecutor(SdVaeSpec.vae.type) { inputs ->
        mapOf("vae" to SdTokens.vae(inputs))
    }
    registrar.registerExecutor(SdModelSpec.model.type) { inputs ->
        val diffusionFields = (inputs["diffusion"] as? WorkflowValue.RecordValue)
            ?.fields?.minus("_type") ?: emptyMap()
        val encoderFields = (inputs["encoders"] as? WorkflowValue.RecordValue)
            ?.fields?.minus("_type") ?: emptyMap()
        val vaeFields = (inputs["vae"] as? WorkflowValue.RecordValue)
            ?.fields?.minus("_type") ?: emptyMap()
        val ownFields = inputs.minus("diffusion").minus("encoders").minus("vae")
        mapOf("model" to SdTokens.model(diffusionFields + encoderFields + vaeFields + ownFields))
    }
    registrar.registerExecutor(SdContextSpec.context.type) { inputs ->
        val modelFields = (inputs["model"] as? WorkflowValue.RecordValue)
            ?.fields?.minus("_type") ?: emptyMap()
        // Merge the optional sub-nodes (seamless/offload/chroma) back into the context record.
        val subPorts = listOf("seamless", "offload", "chroma")
        val subFields = subPorts.fold(emptyMap<String, WorkflowValue>()) { acc, port ->
            acc + ((inputs[port] as? WorkflowValue.RecordValue)?.fields?.minus("_type") ?: emptyMap())
        }
        mapOf("context" to SdTokens.context(
            modelFields + subFields + inputs.minus("model").minus(subPorts.toSet()),
        ))
    }
    registrar.registerExecutor(SdSeamlessSpec.seamless.type) { inputs ->
        mapOf("seamless" to SdTokens.seamless(inputs))
    }
    registrar.registerExecutor(SdChromaSpec.chroma.type) { inputs ->
        mapOf("chroma" to SdTokens.chroma(inputs))
    }
    registrar.registerExecutor(SdOffloadSpec.offload.type) { inputs ->
        mapOf("offload" to SdTokens.offload(inputs))
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
    registrar.registerExecutor(SdControlNetSpec.controlNet.type) { inputs ->
        mapOf("controlnet" to SdTokens.controlNet(inputs))
    }
    registrar.registerExecutor(SdIdCondSpec.idCond.type) { inputs ->
        mapOf("id_cond" to SdTokens.idCond(inputs))
    }
    registrar.registerExecutor(SdServerSpec.server.type) { inputs ->
        mapOf("server" to SdTokens.server(inputs))
    }
}
