package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacingValues
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.math.roundToInt

// The header is the drag handle — no interactive children, so gesture ownership is uncontested.
// A per-node testTag ("node-header-<id>") lets UI tests address a specific node's handle even when
// several nodes share a label.
@Composable
internal fun FieldHeader(
    label: String,
    theme: FieldNodeTheme,
    onMove: ((IntOffset) -> Unit)? = null,
    nodeId: String? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(HEADER_DP.dp).background(theme.headerBackground())
            .then(if (nodeId != null) Modifier.testTag("node-header-$nodeId") else Modifier)
            .padding(horizontal = GraphynSpacingValues.spacing.huge)
            .then(if (onMove != null) Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
                        ?: return@awaitEachGesture
                    drag(down.id) { c ->
                        c.consume()
                        val d = c.position - c.previousPosition
                        onMove(IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
            } else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(label, style = appTheme.typography.nodeHeader.copy(color = theme.titleColor()))
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
        Box(Modifier.fillMaxWidth().height(GraphynSpacingValues.spacing.xs).background(theme.divider()))
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
            BasicText(input.name, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
            Spacer(Modifier.weight(1f))
            BasicText("null", style = appTheme.typography.nodeLabel.copy(color = theme.labelColor().copy(alpha = 0.4f)))
        }
    }
}

private fun defaultForType(type: WorkflowType): WorkflowValue = when (type) {
    WorkflowType.IntType     -> WorkflowValue.IntValue(0)
    WorkflowType.DoubleType  -> WorkflowValue.DoubleValue(0.0)
    WorkflowType.BooleanType -> WorkflowValue.BooleanValue(false)
    else                     -> WorkflowValue.StringValue("")
}
