package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacingValues
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec
import com.ronjunevaldoz.graphyn.core.model.WorkflowType
import com.ronjunevaldoz.graphyn.core.model.WorkflowValue
import kotlin.math.round

private val INT_REGEX = Regex("^-?\\d*$")
private val DOUBLE_REGEX = Regex("^-?\\d*\\.?\\d*$")

@Composable
internal fun NumericRow(
    input: PortSpec,
    currentValue: WorkflowValue?,
    onValueChange: (WorkflowValue) -> Unit,
    theme: FieldNodeTheme,
) {
    val step = if (input.type is WorkflowType.IntType) 1.0 else 0.1
    var editText by remember { mutableStateOf<String?>(null) }
    var focusGranted by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    fun commit() {
        val raw = editText ?: return
        editText = null
        val parsed = parseNumeric(currentValue, raw) ?: return
        onValueChange(parsed)
    }
    LaunchedEffect(editText) { if (editText != null) focusRequester.requestFocus() }
    FieldRow(name = input.name, description = input.description, hasValue = currentValue != null) {
        if (currentValue != null) {
            if (editText != null) {
                BasicTextField(
                    value = editText!!,
                    onValueChange = { if (isValidIntermediate(input.type, it)) editText = it },
                    modifier = Modifier.width(VALUE_DP.dp).focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused) focusGranted = true else if (focusGranted) commit() },
                    textStyle = appTheme.typography.nodeLabel.copy(color = theme.valueText(), textAlign = TextAlign.Center),
                    decorationBox = { inner ->
                        Box(Modifier.clip(RoundedCornerShape(GraphynSpacingValues.spacing.md)).background(theme.valueBg()).padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.sm), Alignment.Center) { inner() }
                    },
                )
            } else {
                StepperChip(currentValue.label(), theme, modifier = Modifier.width(VALUE_DP.dp),
                    onMinus = { stepValue(currentValue, -step)?.let(onValueChange) },
                    onEdit = { focusGranted = false; editText = currentValue.label() },
                    onPlus = { stepValue(currentValue, step)?.let(onValueChange) },
                )
            }
        }
    }
}

@Composable
private fun StepperChip(
    label: String,
    theme: FieldNodeTheme,
    modifier: Modifier = Modifier,
    onMinus: () -> Unit,
    onEdit: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier.clip(RoundedCornerShape(GraphynSpacingValues.spacing.md)).background(theme.valueBg()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable(onClick = onMinus).padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.sm)) {
            BasicText("−", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
        }
        Box(Modifier.width(GraphynSpacingValues.spacing.xs).height(GraphynSpacingValues.spacing.huge).background(theme.divider()))
        Box(Modifier.weight(1f).clickable(onClick = onEdit).padding(horizontal = GraphynSpacingValues.spacing.lg, vertical = GraphynSpacingValues.spacing.sm), Alignment.Center) {
            BasicText(label, style = appTheme.typography.nodeLabel.copy(color = theme.valueText(), textAlign = TextAlign.Center))
        }
        Box(Modifier.width(GraphynSpacingValues.spacing.xs).height(GraphynSpacingValues.spacing.huge).background(theme.divider()))
        Box(Modifier.clickable(onClick = onPlus).padding(horizontal = GraphynSpacingValues.spacing.xl, vertical = GraphynSpacingValues.spacing.sm)) {
            BasicText("+", style = appTheme.typography.nodeLabel.copy(color = theme.valueText()))
        }
    }
}

private fun isValidIntermediate(type: WorkflowType, text: String): Boolean = when (type) {
    WorkflowType.IntType -> text.isEmpty() || INT_REGEX.matches(text)
    WorkflowType.DoubleType -> text.isEmpty() || DOUBLE_REGEX.matches(text)
    else -> true
}

private fun stepValue(current: WorkflowValue, delta: Double): WorkflowValue? = when (current) {
    is WorkflowValue.IntValue -> WorkflowValue.IntValue(current.value + delta.toInt())
    is WorkflowValue.DoubleValue -> WorkflowValue.DoubleValue(round((current.value + delta) * 1000) / 1000.0)
    else -> null
}

private fun parseNumeric(original: WorkflowValue?, raw: String): WorkflowValue? = when (original) {
    is WorkflowValue.IntValue -> raw.toIntOrNull()?.let { WorkflowValue.IntValue(it) }
    is WorkflowValue.DoubleValue -> raw.toDoubleOrNull()?.let { WorkflowValue.DoubleValue(it) }
    else -> null
}
