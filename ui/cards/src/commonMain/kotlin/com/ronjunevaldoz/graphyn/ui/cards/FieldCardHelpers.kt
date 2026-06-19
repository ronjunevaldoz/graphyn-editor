package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue

@Composable
internal fun FieldHeader(label: String, theme: FieldNodeTheme) {
    Box(
        modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp).background(theme.headerBackground())
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(label, style = TextStyle(color = theme.titleColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
internal fun FieldBody(
    inputs: List<PortSpec>,
    values: Map<String, WorkflowValue>,
    onValueChange: (key: String, value: WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    inputs.forEach { input ->
        val value = values[input.name]
        val onChange: (WorkflowValue) -> Unit = { onValueChange(input.name, it) }
        when (val type = input.type) {
            is WorkflowType.EnumType      -> SingleSelectRow(input, value, type.values, onChange, theme)
            is WorkflowType.MultiEnumType -> MultiSelectRow(input, value, type.values, onChange, theme)
            is WorkflowType.ListType      -> ListRow(input, value, type.elementType, onChange, theme)
            is WorkflowType.RecordType    -> RecordRow(input, value, type.fields, onChange, theme)
            is WorkflowType.NullableType  -> NullableRow(input, value, type.wrappedType, onChange, theme)
            WorkflowType.IntType, WorkflowType.DoubleType -> NumericRow(input, value, onChange, theme)
            else -> InputRow(input, value, onChange, theme)
        }
    }
}

@Composable
internal fun FieldFooter(outputs: List<PortSpec>, theme: FieldNodeTheme) {
    Box(Modifier.fillMaxWidth().height(FOOTER_DIVIDER_DP.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.divider()))
    }
    outputs.forEach { output -> OutputRow(output = output, theme = theme) }
}

@Composable
private fun NullableRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    innerType: WorkflowType,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    val isNull = currentValue == null || currentValue is WorkflowValue.NullValue
    Row(
        modifier = Modifier.fillMaxWidth().height(ROW_DP.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (!isNull) theme.selectedBorder() else theme.valueBg())
                .clickable {
                    if (isNull) {
                        onValueChange(defaultForType(innerType))
                    } else {
                        onValueChange(WorkflowValue.NullValue)
                    }
                },
        )
        Spacer(Modifier.width(6.dp))
        if (!isNull) {
            val innerInput = input.copy(type = innerType)
            val innerOnChange: (WorkflowValue) -> Unit = onValueChange
            when (innerType) {
                WorkflowType.IntType, WorkflowType.DoubleType -> NumericRow(innerInput, currentValue, innerOnChange, theme)
                else -> InputRow(innerInput, currentValue, innerOnChange, theme)
            }
        } else {
            BasicText(input.name, style = TextStyle(color = theme.labelColor(), fontSize = 10.sp))
            Spacer(Modifier.weight(1f))
            BasicText("null", style = TextStyle(color = theme.labelColor().copy(alpha = 0.4f), fontSize = 10.sp))
        }
    }
}

private fun defaultForType(type: WorkflowType): WorkflowValue = when (type) {
    WorkflowType.IntType     -> WorkflowValue.IntValue(0)
    WorkflowType.DoubleType  -> WorkflowValue.DoubleValue(0.0)
    WorkflowType.BooleanType -> WorkflowValue.BooleanValue(false)
    else                     -> WorkflowValue.StringValue("")
}
