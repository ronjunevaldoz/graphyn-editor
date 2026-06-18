package com.ronjunevaldoz.graphyn.editor.shell.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.model.ValidationError
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun GraphynValidationSummary(errors: List<ValidationError>) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.danger.copy(alpha = 0.08f))
            .border(1.dp, colors.danger.copy(alpha = 0.3f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            "${errors.size} validation issue${if (errors.size == 1) "" else "s"}",
            style = type.label.copy(color = colors.danger),
        )
        errors.take(4).forEach { error ->
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                BasicText(error.code, style = type.labelSmall.copy(color = colors.danger.copy(alpha = 0.8f)))
                BasicText(error.message, style = type.bodySmall.copy(color = colors.textSecondary))
            }
        }
    }
}
