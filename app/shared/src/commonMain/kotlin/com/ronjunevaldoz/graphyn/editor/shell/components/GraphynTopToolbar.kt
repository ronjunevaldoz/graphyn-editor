package com.ronjunevaldoz.graphyn.editor.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.editor.design.GraphynDs
import com.ronjunevaldoz.graphyn.editor.design.components.GdsText
import com.ronjunevaldoz.graphyn.editor.theme.GraphynAppearanceState
import com.ronjunevaldoz.graphyn.editor.theme.GraphynBranding

@Composable
internal fun GraphynTopToolbar(
    modifier: Modifier = Modifier,
    branding: GraphynBranding,
    appearanceState: GraphynAppearanceState,
    canRun: Boolean,
    onRun: () -> Unit,
    onAutoLayout: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    workflowName: String? = null,
) {
    val colors = GraphynDs.colors
    val type = GraphynDs.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.panelBackground)
            .border(width = 1.dp, color = colors.border)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onHome != null) {
            val homeInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = homeInteraction, indication = null, onClick = onHome)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("← Home", style = type.label.copy(color = colors.accent))
            }
        }
        branding.logo?.let { logo ->
            Image(
                painter = logo,
                contentDescription = branding.appName,
                modifier = Modifier.size(20.dp),
            )
        }
        BasicText(
            text = workflowName ?: branding.appName,
            style = type.appTitle.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.weight(1f))
        ThemeControls(appearanceState = appearanceState)
        if (onAutoLayout != null) {
            val autoInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.panelBackground)
                    .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(6.dp))
                    .clickable(interactionSource = autoInteraction, indication = null, onClick = onAutoLayout)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = "Auto Layout", style = type.label.copy(color = colors.textSecondary))
            }
        }
        if (canRun) {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.accent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onRun,
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Execute",
                    style = type.label.copy(color = colors.accentForeground),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}
