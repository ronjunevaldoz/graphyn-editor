package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    toastHostState: ToastHostState = remember { ToastHostState() },
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    CompositionLocalProvider(LocalToastHostState provides toastHostState) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                topBar?.invoke()
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues())
                }
                bottomBar?.invoke()
            }
            ToastHost(
                toastHostState = toastHostState,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            )
        }
    }
}
