package com.ronjunevaldoz.graphyn.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.AppColors
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.AppShapes
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.AppSpacing
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.AppTypography
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.DarkAppColors
import com.ronjunevaldoz.graphyn.core.designsystem.tokens.LightAppColors

@Immutable
data class AppTheme(
    val colors: AppColors,
    val typography: AppTypography,
    val shapes: AppShapes,
    val spacing: AppSpacing,
) {
    companion object {
        val Local: ProvidableCompositionLocal<AppTheme> =
            staticCompositionLocalOf { light() }

        fun light(
            colors: AppColors         = LightAppColors,
            typography: AppTypography = AppTypography(),
            shapes: AppShapes         = AppShapes(),
            spacing: AppSpacing       = AppSpacing(),
        ) = AppTheme(colors, typography, shapes, spacing)

        fun dark(
            colors: AppColors         = DarkAppColors,
            typography: AppTypography = AppTypography(),
            shapes: AppShapes         = AppShapes(),
            spacing: AppSpacing       = AppSpacing(),
        ) = AppTheme(colors, typography, shapes, spacing)
    }
}

@Composable
fun AppTheme(
    dark: Boolean = true,
    theme: AppTheme = if (dark) AppTheme.dark() else AppTheme.light(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        AppTheme.Local provides theme,
        content = content,
    )
}

val appTheme: AppTheme
    @Composable @ReadOnlyComposable get() = AppTheme.Local.current
