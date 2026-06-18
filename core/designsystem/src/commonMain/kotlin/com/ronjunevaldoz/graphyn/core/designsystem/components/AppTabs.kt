package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

enum class AppTabVariant { Line, Pill, Enclosed }

@Composable
fun AppTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: AppTabVariant = AppTabVariant.Line,
    content: (@Composable (selectedIndex: Int) -> Unit)? = null,
) {
    val theme = appTheme
    Column(modifier = modifier) {
        when (variant) {
            AppTabVariant.Line -> LineTabRow(tabs, selectedIndex, onTabSelected, theme.colors.primary, theme.colors.onSurface, theme.colors.onSurfaceVariant)
            AppTabVariant.Pill -> PillTabRow(tabs, selectedIndex, onTabSelected, theme)
            AppTabVariant.Enclosed -> EnclosedTabRow(tabs, selectedIndex, onTabSelected, theme)
        }
        if (content != null) {
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "tabContent",
            ) { index -> content(index) }
        }
    }
}

@Composable
private fun LineTabRow(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit, indicator: Color, selected: Color, unselected: Color) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onTabSelected(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppText(title, style = AppTextStyle.LabelLarge, color = if (isSelected) selected else unselected, modifier = Modifier.padding(vertical = 10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (isSelected) indicator else Color.Transparent))
                }
            }
        }
        AppSeparator(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PillTabRow(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit, theme: com.ronjunevaldoz.graphyn.core.designsystem.theme.AppTheme) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(theme.colors.secondary, RoundedCornerShape(theme.shapes.full))
            .padding(4.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(theme.shapes.full))
                    .background(if (isSelected) theme.colors.background else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onTabSelected(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { AppText(title, style = AppTextStyle.LabelLarge, color = if (isSelected) theme.colors.onSurface else theme.colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun EnclosedTabRow(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit, theme: com.ronjunevaldoz.graphyn.core.designsystem.theme.AppTheme) {
    Row(modifier = Modifier.fillMaxWidth().background(theme.colors.surfaceVariant).padding(horizontal = 16.dp)) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            AppText(
                title,
                style = AppTextStyle.LabelLarge,
                color = if (isSelected) theme.colors.onSurface else theme.colors.onSurfaceVariant,
                modifier = Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onTabSelected(index) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
