package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import com.ronjunevaldoz.graphyn.core.model.displayName
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun OutputRow(portName: String, value: WorkflowValue) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BasicText(portName, style = type.bodySmall.copy(color = colors.textPrimary))
        BasicText(
            text = value.preview(),
            style = type.mono.copy(color = colors.accent),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun WorkflowValue.preview(): String = when (this) {
    is WorkflowValue.StringValue  -> if (value.length > 40) "\"${value.take(37)}…\"" else "\"$value\""
    is WorkflowValue.IntValue     -> value.toString()
    is WorkflowValue.DoubleValue  -> value.toString()
    is WorkflowValue.BooleanValue -> value.toString()
    is WorkflowValue.ListValue    -> "[${items.size} items]"
    is WorkflowValue.RecordValue  -> "{${fields.size} fields}"
    WorkflowValue.NullValue       -> "null"
    WorkflowValue.OpaqueValue     -> "opaque"
}

@Composable
internal fun PortRow(port: PortSpec, isInput: Boolean) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(
                text = "${if (isInput) "→" else "←"} ${port.name}",
                style = type.bodySmall.copy(color = colors.textPrimary),
            )
            BasicText(
                text = port.type.displayName(),
                style = type.mono.copy(color = colors.textDisabled),
            )
        }
        port.description?.let {
            BasicText(it, style = type.bodySmall.copy(color = colors.textSecondary))
        }
    }
}
