package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.NullableType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType

/**
 * Node spec for `sd.encoders` — text encoder paths for a stable-diffusion.cpp context.
 *
 * Wire encoders → [SdModelSpec.model]. Separate from the diffusion model so you can swap
 * encoders (e.g. use a different T5-XXL quantisation) without changing the diffusion path.
 *
 * For SD1.5/SDXL checkpoints that bundle encoders, this node is optional — omit it and
 * let the checkpoint supply its own encoders.
 */
object SdEncodersSpec {
    val encoders = NodeSpec(
        type = "sd.encoders",
        label = "SD Encoders",
        description = "Text encoder model paths. Wire encoders → sd.model.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("clip_l_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--clip_l: CLIP-L text encoder (SD1/SD2/SDXL/FLUX)."),
            PortSpec("clip_g_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--clip_g: CLIP-G text encoder (SDXL/SD3)."),
            PortSpec("clip_vision_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--clip_vision: CLIP vision encoder (PhotoMaker/PuLID/Qwen)."),
            PortSpec("t5xxl_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--t5xxl: T5-XXL text encoder (SD3/FLUX/Chroma)."),
            PortSpec("llm_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--llm: LLM text encoder (Wan2.1/Wan2.2/LTX-2.3)."),
            PortSpec("llm_vision_path", NullableType(StringType), portColor = COLOR_STRING,
                description = "--llm_vision: LLM vision encoder."),
        ),
        outputs = listOf(
            PortSpec("encoders", OpaqueType, portColor = COLOR_ENCODERS,
                description = "Opaque encoder-paths token consumed by sd.model."),
        ),
    )
}
