package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.offload` — VRAM/CPU-offload tuning, pulled out of `sd.context`.
 *
 * Wire to `sd.context`'s `offload` port to fit large models on a small GPU. When absent, the server
 * defaults `max_vram` to auto (-1), so most workflows don't need this node. Fields merge back into
 * the context at runtime.
 */
object SdOffloadSpec {
    val offload = NodeSpec(
        type = "sd.offload",
        label = "SD Offload",
        description = "VRAM budget + CPU offload (max_vram graph-cut, clip/vae on CPU). Wire to sd.context's offload port.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("max_vram", NullableType(DoubleType), portColor = COLOR_FLOAT,
                description = "--max-vram <GiB>: VRAM budget for graph-cut offload. 0 = off, -1 = auto."),
            PortSpec("offload_params_to_cpu", BooleanType, portColor = COLOR_BOOL,
                description = "--offload-to-cpu: offload model params to CPU RAM between steps (slower, less VRAM)."),
            PortSpec("clip_on_cpu", BooleanType, portColor = COLOR_BOOL,
                description = "--clip-on-cpu: run the text encoder on CPU to free VRAM."),
            PortSpec("vae_on_cpu", BooleanType, portColor = COLOR_BOOL,
                description = "--vae-on-cpu: run the VAE on CPU to free VRAM."),
            PortSpec("stream_layers", BooleanType, portColor = COLOR_BOOL,
                description = "--stream-layers: layer residency + prefetch on top of max_vram."),
            PortSpec("eager_load", BooleanType, portColor = COLOR_BOOL,
                description = "--eager-load: load all params at model-load time instead of lazily."),
        ),
        outputs = listOf(
            PortSpec("offload", OpaqueType, portColor = COLOR_CONTEXT,
                description = "Opaque offload config; connect to sd.context's offload input."),
        ),
        defaultValues = mapOf(
            "offload_params_to_cpu" to WorkflowValue.BooleanValue(false),
            "clip_on_cpu"           to WorkflowValue.BooleanValue(false),
            "vae_on_cpu"            to WorkflowValue.BooleanValue(false),
            "stream_layers"         to WorkflowValue.BooleanValue(false),
            "eager_load"            to WorkflowValue.BooleanValue(false),
        ),
    )
}
