package com.ronjunevaldoz.graphyn.plugins.stylenodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.NodeSpec
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.math.max

internal val PORT_DOT = 6.dp
internal val PORT_GAP = 4.dp

internal fun PortSpec.portColor(): Color = portColor?.let { Color(it) } ?: NODE_MUTED

@Composable
internal fun FieldHeader(label: String, theme: FieldNodeTheme) {
    Box(
        modifier = Modifier.fillMaxWidth().background(theme.headerBackground())
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        BasicText(label, style = TextStyle(color = theme.titleColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
internal fun FieldBody(spec: NodeSpec, defaults: Map<String, WorkflowValue>, theme: FieldNodeTheme) {
    val rowCount = max(spec.inputs.size, spec.outputs.size)
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        repeat(rowCount) { i ->
            PortRow(
                input = spec.inputs.getOrNull(i),
                output = spec.outputs.getOrNull(i),
                defaultVal = spec.inputs.getOrNull(i)?.let { defaults[it.name]?.label() },
                theme = theme,
            )
        }
    }
}

@Composable
internal fun PortRow(input: PortSpec?, output: PortSpec?, defaultVal: String?, theme: FieldNodeTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (input != null) {
            Box(Modifier.size(PORT_DOT).clip(CircleShape).background(input.portColor()))
            Spacer(Modifier.width(PORT_GAP))
            BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
        }
        Spacer(Modifier.weight(1f))
        if (defaultVal != null) {
            Box(
                modifier = Modifier.widthIn(min = 40.dp).clip(RoundedCornerShape(3.dp))
                    .background(theme.valueBg()).padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText(defaultVal, style = TextStyle(color = theme.valueText(), fontSize = 10.sp)) }
            Spacer(Modifier.width(PORT_GAP))
        }
        if (output != null) {
            BasicText(output.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
            Spacer(Modifier.width(PORT_GAP))
            Box(Modifier.size(PORT_DOT).clip(CircleShape).background(output.portColor()))
        }
    }
}

@Composable
internal fun FieldFooter(theme: FieldNodeTheme) {
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(1.dp).background(theme.divider()))
    Spacer(Modifier.height(4.dp))
}

internal fun WorkflowValue.label(): String = when (this) {
    is WorkflowValue.IntValue     -> value.toString()
    is WorkflowValue.DoubleValue  -> (kotlin.math.round(value * 1000) / 1000.0).toString()
    is WorkflowValue.StringValue  -> value
    is WorkflowValue.BooleanValue -> if (value) "true" else "false"
    else -> ""
}
