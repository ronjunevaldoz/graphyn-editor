package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun WorkflowSchemaView(nodeSpecs: List<NodeSpec>) {
    val grouped = remember(nodeSpecs) {
        nodeSpecs.sortedBy { it.label }
            .groupBy { it.category ?: "Uncategorized" }
            .toSortedMap()
    }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        grouped.forEach { (category, specs) ->
            LauncherSection(category) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    specs.forEach { WorkflowSchemaCard(it) }
                }
            }
        }
        if (grouped.isEmpty()) {
            BasicText("No node specs are installed.", style = GraphynDs.type.bodySmall.copy(color = GraphynDs.colors.textDisabled))
        }
    }
}

@Composable
private fun WorkflowSchemaCard(spec: NodeSpec) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(colors.surfaceCard).border(1.dp, colors.border, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicText(spec.label, style = type.nodeTitle.copy(color = colors.textPrimary))
            BasicText(spec.type, style = type.mono.copy(color = colors.textDisabled))
        }
        spec.description?.let { BasicText(it, style = type.bodySmall.copy(color = colors.textSecondary)) }
        if (spec.inputs.isNotEmpty()) SchemaPortBlock("Inputs", spec.inputs)
        if (spec.outputs.isNotEmpty()) SchemaPortBlock("Outputs", spec.outputs)
        if (spec.defaultValues.isNotEmpty()) {
            BasicText("Defaults: ${spec.defaultValues.entries.sortedBy { it.key }.joinToString(" · ") { "${it.key}=${it.value.schemaValueText()}" }}", style = type.mono.copy(color = colors.textSecondary))
        }
    }
}

@Composable
private fun SchemaPortBlock(label: String, ports: List<PortSpec>) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = type.labelSmall.copy(color = colors.textDisabled))
        ports.forEach { port ->
            BasicText(
                "${port.name}: ${port.type.schemaTypeText()}${if (port.required) "" else " (optional)"}",
                style = type.bodySmall.copy(color = colors.textSecondary),
            )
            port.description?.let { BasicText(it, style = type.mono.copy(color = colors.textDisabled)) }
        }
    }
}

private fun WorkflowType.schemaTypeText(): String = when (this) {
    WorkflowType.StringType -> "String"
    WorkflowType.IntType -> "Int"
    WorkflowType.DoubleType -> "Double"
    WorkflowType.BooleanType -> "Boolean"
    WorkflowType.OpaqueType -> "Any"
    is WorkflowType.ListType -> "List<${elementType.schemaTypeText()}>"
    is WorkflowType.NullableType -> "${wrappedType.schemaTypeText()}?"
    is WorkflowType.RecordType -> "Record"
    is WorkflowType.EnumType -> "Enum(${values.joinToString()})"
    is WorkflowType.MultiEnumType -> "MultiEnum(${values.joinToString()})"
}

private fun WorkflowValue.schemaValueText(): String = when (this) {
    is WorkflowValue.StringValue -> value
    is WorkflowValue.IntValue -> value.toString()
    is WorkflowValue.DoubleValue -> value.toString()
    is WorkflowValue.BooleanValue -> value.toString()
    is WorkflowValue.ListValue -> items.joinToString(prefix = "[", postfix = "]") { it.schemaValueText() }
    is WorkflowValue.RecordValue -> fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key=${value.schemaValueText()}" }
    WorkflowValue.NullValue -> "null"
    WorkflowValue.OpaqueValue -> "opaque"
}
