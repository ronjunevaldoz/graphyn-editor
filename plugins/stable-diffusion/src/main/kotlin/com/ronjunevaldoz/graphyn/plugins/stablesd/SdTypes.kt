package com.ronjunevaldoz.graphyn.plugins.stablesd

import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.NullValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.RecordValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.StringValue
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue.ListValue

/**
 * Opaque token conventions for stable-diffusion.cpp nodes.
 *
 * Opaque output ports carry [RecordValue] maps under the hood so executors can extract
 * params without needing a shared mutable context. The token key marks the type so
 * executors can validate the correct node is wired.
 */
internal object SdTokens {
    fun context(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.context")) + inputs.mapValues { it.value })

    fun seamless(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.seamless")) + inputs.mapValues { it.value })

    fun sampler(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.sampler")) + inputs.mapValues { it.value })

    fun lora(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.lora")) + inputs.mapValues { it.value })

    fun hires(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.hires")) + inputs.mapValues { it.value })

    fun cache(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.cache")) + inputs.mapValues { it.value })

    fun model(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.model")) + inputs.mapValues { it.value })

    fun controlNet(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.controlnet")) + inputs.mapValues { it.value })

    fun idCond(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.id_cond")) + inputs.mapValues { it.value })

    fun vaeTiling(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.vae_tiling")) + inputs.mapValues { it.value })

    fun encoders(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.encoders")) + inputs.mapValues { it.value })

    fun vae(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.vae")) + inputs.mapValues { it.value })

    fun diffusion(inputs: Map<String, WorkflowValue>): WorkflowValue =
        RecordValue(mapOf("_type" to StringValue("sd.diffusion")) + inputs.mapValues { it.value })

    fun RecordValue.fields(): Map<String, WorkflowValue> = fields

    fun WorkflowValue.asRecord(expectType: String): Map<String, WorkflowValue> {
        require(this is RecordValue) { "Expected opaque $expectType token, got ${this::class.simpleName}" }
        val type = (fields["_type"] as? StringValue)?.value
        require(type == expectType) { "Expected _type=$expectType, got $type" }
        return fields
    }

    fun WorkflowValue?.orEmpty(): Map<String, WorkflowValue> = when (this) {
        is RecordValue -> fields
        is NullValue, null -> emptyMap()
        else -> emptyMap()
    }
}
