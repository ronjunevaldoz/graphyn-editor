package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

data class NavBarItem(
    val label: String,
    val painter: Painter,
    val selectedPainter: Painter = painter,
    val contentDescription: String? = null,
)

@Composable
fun AppNavigationBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = appTheme.colors.background,
) {
    val theme = appTheme
    Column(modifier = modifier.fillMaxWidth().background(backgroundColor)) {
        AppSeparator()
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onItemSelected(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppIcon(
                        painter = if (selected) item.selectedPainter else item.painter,
                        contentDescription = item.contentDescription ?: item.label,
                        size = IconSize.Md,
                        tint = if (selected) theme.colors.primary else theme.colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    AppText(
                        text = item.label,
                        style = AppTextStyle.LabelSmall,
                        color = if (selected) theme.colors.primary else theme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
