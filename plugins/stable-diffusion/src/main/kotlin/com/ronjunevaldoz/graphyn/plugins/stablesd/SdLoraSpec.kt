package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.BooleanType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.DoubleType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.OpaqueType
import com.ronjunevaldoz.graphyn.core.model.WorkflowType.StringType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

/**
 * Node spec for `sd.lora` — represents a single LoRA entry (`sd_lora_t`).
 *
 * Chain multiple `sd.lora` nodes and collect them into a `List` port on generation nodes.
 * The path is resolved relative to `lora_model_dir` set in [SdContextSpec.context].
 */
object SdLoraSpec {
    val lora = NodeSpec(
        type = "sd.lora",
        label = "SD LoRA",
        description = "A single LoRA (Low-Rank Adaptation) entry. Chain multiple into a list for generation nodes.",
        category = CATEGORY_SD_CFG,
        inputs = listOf(
            PortSpec("path", StringType, portColor = COLOR_STRING,
                description = "Path to the LoRA .safetensors file, or a name resolvable under lora_model_dir."),
            PortSpec("multiplier", DoubleType, portColor = COLOR_FLOAT,
                description = "LoRA weight multiplier. 1.0 = full strength; negative values invert the effect."),
            PortSpec("is_high_noise", BooleanType, portColor = COLOR_BOOL,
                description = "When true, this LoRA applies to the high-noise diffusion model path (MoE video models only)."),
        ),
        outputs = listOf(
            PortSpec("lora", OpaqueType, portColor = COLOR_LORA,
                description = "Opaque LoRA entry. Collect into a list port on sd.txt2img / sd.img2img / sd.txt2vid / sd.img2vid."),
        ),
        defaultValues = mapOf(
            "multiplier"    to WorkflowValue.DoubleValue(1.0),
            "is_high_noise" to WorkflowValue.BooleanValue(false),
        ),
    )
}
