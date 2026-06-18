package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.ronjunevaldoz.graphyn.core.designsystem.theme.appTheme

class PopoverState {
    var isOpen by mutableStateOf(false)
        private set
    fun toggle() { isOpen = !isOpen }
    fun open()   { isOpen = true }
    fun close()  { isOpen = false }
}

@Composable
fun AppPopover(
    modifier: Modifier = Modifier,
    trigger: @Composable (state: PopoverState) -> Unit,
    content: @Composable () -> Unit,
) {
    val theme = appTheme
    val shape = RoundedCornerShape(theme.shapes.lg)
    val state = remember { PopoverState() }

    Box(modifier = modifier) {
        trigger(state)
        if (state.isOpen) {
            Popup(onDismissRequest = { state.close() }) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, shape)
                        .background(theme.colors.surface, shape)
                        .border(1.dp, theme.colors.border, shape)
                        .padding(16.dp),
                ) {
                    content()
                }
            }
        }
    }
}
