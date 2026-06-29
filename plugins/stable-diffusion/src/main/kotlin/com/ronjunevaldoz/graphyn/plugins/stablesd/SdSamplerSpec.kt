package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.EnumType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.IntType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.ListType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.sampler` — encapsulates `sd_sample_params_t` including guidance and SLG.
 *
 * Output is an opaque token that generation nodes accept on their `sampler` input. Wire one
 * sampler to multiple generation nodes so they share the same sampling configuration.
 *
 * Defaults match `sd_sample_params_init()` in `stable-diffusion.cpp`.
 */
object SdSamplerSpec {
    val sampler = NodeSpec(
        type = "sd.sampler",
        label = "SD Sampler",
        description = "Configures sampling parameters (steps, method, scheduler, CFG) for stable-diffusion.cpp generation nodes.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("sample_method", NullableType(EnumType(SD_SAMPLE_METHODS)), portColor = COLOR_SAMPLER,
                description = "--sampling-method: Noise sampling algorithm. Null = model default."),
            PortSpec("scheduler", NullableType(EnumType(SD_SCHEDULERS)), portColor = COLOR_SAMPLER,
                description = "--scheduler: Noise schedule. Null = model default."),
            PortSpec("sample_steps", IntType, portColor = COLOR_INT,
                description = "--steps: Number of denoising steps. Default: 20."),
            PortSpec("txt_cfg", DoubleType, portColor = COLOR_FLOAT,
                description = "--cfg-scale: Text classifier-free guidance scale. Default: 7.0."),
            PortSpec("img_cfg", NullableType(DoubleType), portColor = COLOR_FLOAT,
                description = "--img-cfg-scale: Image CFG scale for img2img (PAG/APG models). Null = infinity (disabled)."),
            PortSpec("distilled_guidance", DoubleType, portColor = COLOR_FLOAT,
                description = "--guidance: Distilled guidance scale (Flux/Wan/LTX models). Default: 3.5."),
            PortSpec("eta", NullableType(DoubleType), portColor = COLOR_FLOAT,
                description = "--eta: Eta parameter for DDIM/DDPM samplers. Null = infinity (disabled)."),
            PortSpec("flow_shift", NullableType(DoubleType), portColor = COLOR_FLOAT,
                description = "--flow-shift: Flow shift parameter (FLUX/Wan/LTX). Null = model default."),
            PortSpec("shifted_timestep", IntType, portColor = COLOR_INT,
                description = "--timestep-shift: Timestep shift for the noise schedule. Default: 0."),
            PortSpec("custom_sigmas", NullableType(ListType(DoubleType)), portColor = COLOR_FLOAT,
                description = "--sigmas: Custom sigma schedule as a list of doubles, overriding scheduler sigmas."),
            PortSpec("extra_sample_args", NullableType(StringType), portColor = COLOR_STRING,
                description = "--extra-sample-args: Additional sampler arguments passed verbatim to sd-cli."),
            // Skip-Layer Guidance (SLG) — sd_slg_params_t embedded in sd_guidance_params_t
            PortSpec("slg_layers", NullableType(ListType(IntType)), portColor = COLOR_FLOAT,
                description = "--skip-layers: Transformer layer indices to skip for SLG (e.g. [7,8,9]). Null = disabled."),
            PortSpec("slg_layer_start", DoubleType, portColor = COLOR_FLOAT,
                description = "--skip-layer-start: Denoising fraction at which SLG begins. Default: 0.01."),
            PortSpec("slg_layer_end", DoubleType, portColor = COLOR_FLOAT,
                description = "--skip-layer-end: Denoising fraction at which SLG ends. Default: 0.2."),
            PortSpec("slg_scale", DoubleType, portColor = COLOR_FLOAT,
                description = "--slg-scale: Skip-layer guidance scale. 0.0 = disabled."),
        ),
        outputs = listOf(
            PortSpec("sampler", OpaqueType, portColor = COLOR_SAMPLER,
                description = "Opaque sampling config passed to generation nodes."),
        ),
        defaultValues = mapOf(
            "sample_steps"      to WorkflowValue.IntValue(20),
            "txt_cfg"           to WorkflowValue.DoubleValue(7.0),
            "distilled_guidance" to WorkflowValue.DoubleValue(3.5),
            "shifted_timestep"  to WorkflowValue.IntValue(0),
            "slg_layer_start"   to WorkflowValue.DoubleValue(0.01),
            "slg_layer_end"     to WorkflowValue.DoubleValue(0.2),
            "slg_scale"         to WorkflowValue.DoubleValue(0.0),
        ),
    )
}
