package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

@Composable
fun AppSelect(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select…",
    enabled: Boolean = true,
) {
    val theme = appTheme
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(theme.shapes.md)

    Box(modifier = modifier.zIndex(if (expanded) 1f else 0f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(theme.colors.background)
                .border(if (expanded) 2.dp else 1.dp, if (expanded) theme.colors.borderFocus else theme.colors.border, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.DropdownList,
                    onClick = { expanded = !expanded },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = selected ?: placeholder,
                style = AppTextStyle.BodyMedium,
                color = if (selected != null) theme.colors.onSurface else theme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            AppText(text = if (expanded) "▲" else "▼", style = AppTextStyle.LabelSmall, muted = true)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(100)) + expandVertically(tween(100)),
            exit = fadeOut(tween(80)) + shrinkVertically(tween(80)),
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .shadow(8.dp, shape)
                    .background(theme.colors.background, shape)
                    .border(1.dp, theme.colors.border, shape)
                    .padding(vertical = 4.dp),
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onSelect(option); expanded = false })
                            .background(if (option == selected) theme.colors.secondary else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(text = option, style = AppTextStyle.BodyMedium, modifier = Modifier.weight(1f))
                        if (option == selected) AppText(text = "✓", style = AppTextStyle.LabelSmall, color = theme.colors.primary)
                    }
                }
            }
        }
    }
}
