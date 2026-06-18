package com.ronjunevaldoz.graphyn.core.designsystem.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf

enum class ToastVariant { Default, Destructive, Success, Warning }

data class ToastData(
    val id: String,
    val title: String,
    val description: String? = null,
    val variant: ToastVariant = ToastVariant.Default,
    val durationMs: Long = 3000L,
)

@Stable
class ToastHostState {
    val toasts = mutableStateListOf<ToastData>()

    fun show(
        title: String,
        description: String? = null,
        variant: ToastVariant = ToastVariant.Default,
        durationMs: Long = 3000L,
    ) {
        val id = "${toasts.size}-${toasts.hashCode()}"
        toasts.add(ToastData(id = id, title = title, description = description, variant = variant, durationMs = durationMs))
    }

    fun dismiss(id: String) {
        toasts.removeAll { it.id == id }
    }
}

val LocalToastHostState = compositionLocalOf { ToastHostState() }
