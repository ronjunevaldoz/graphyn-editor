package com.ronjunevaldoz.graphyn.editor.launcher

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.store.WorkflowMeta
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs

@Composable
internal fun LauncherSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val type = GraphynDs.type
    val colors = GraphynDs.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(title.uppercase(), style = type.labelSmall.copy(color = colors.textDisabled))
        content()
    }
}

@Composable
internal fun NewWorkflowButton(onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(6.dp)
    BasicText(
        "New Workflow",
        style = type.labelSmall.copy(color = colors.accent),
        modifier = Modifier
            .clip(shape)
            .border(1.dp, colors.accent, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
internal fun SavedWorkflowCard(meta: WorkflowMeta, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(meta.name, style = type.nodeTitle.copy(color = colors.textPrimary))
        if (meta.description.isNotEmpty()) {
            BasicText(meta.description, style = type.bodySmall.copy(color = colors.textSecondary))
        }
        BasicText("v${meta.versionCount}", style = type.mono.copy(color = colors.textDisabled))
    }
}
