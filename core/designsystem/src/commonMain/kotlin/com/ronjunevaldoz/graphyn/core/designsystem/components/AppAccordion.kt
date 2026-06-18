package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class AccordionItem(
    val title: String,
    val content: @Composable () -> Unit,
)

@Composable
fun AppAccordion(
    items: List<AccordionItem>,
    modifier: Modifier = Modifier,
    multiExpand: Boolean = false,
) {
    val expandedIndices = remember { mutableStateOf(setOf<Int>()) }

    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val isExpanded = index in expandedIndices.value
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(200),
                label = "chevron$index",
            )

            Column {
                if (index > 0) AppSeparator()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                expandedIndices.value = if (isExpanded) {
                                    expandedIndices.value - index
                                } else {
                                    if (multiExpand) expandedIndices.value + index else setOf(index)
                                }
                            },
                        )
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(text = item.title, style = AppTextStyle.LabelLarge)
                    Spacer(Modifier.width(8.dp))
                    AppText(
                        text = "▼",
                        style = AppTextStyle.LabelSmall,
                        muted = true,
                        modifier = Modifier.graphicsLayer { rotationZ = rotation },
                    )
                }
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(tween(200)),
                    exit = shrinkVertically(tween(200)),
                ) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) { item.content() }
                }
            }
        }
        AppSeparator()
    }
}
