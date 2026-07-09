package com.ronjunevaldoz.graphyn.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.GraphynSpacingValues
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme
import com.ronjunevaldoz.graphyn.core.model.PortSpec

@Composable
internal fun SubgraphBoundaryBody(
    inputs: List<PortSpec>,
    outputs: List<PortSpec>,
    theme: FieldNodeTheme,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (inputs.isNotEmpty()) {
            SectionLabel("Inputs", theme)
            inputs.forEach { input ->
                FieldRow(name = input.name, description = input.description, hasValue = false) {}
            }
        }
        if (inputs.isNotEmpty() && outputs.isNotEmpty()) {
            Spacer(Modifier.fillMaxWidth().height(FOOTER_DIVIDER_DP.dp).background(theme.divider()))
        }
        if (outputs.isNotEmpty()) {
            SectionLabel("Outputs", theme)
            outputs.forEach { output ->
                FieldRow(name = output.name, description = output.description) {}
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, theme: FieldNodeTheme) {
    Box(Modifier.fillMaxWidth().height(SUBGRAPH_SECTION_DP.dp).padding(horizontal = GraphynSpacingValues.spacing.sm)) {
        BasicText(text, style = appTheme.typography.nodeLabel.copy(color = theme.labelColor()))
    }
}
