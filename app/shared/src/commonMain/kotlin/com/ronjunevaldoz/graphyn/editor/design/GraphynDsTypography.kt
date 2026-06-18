package com.ronjunevaldoz.graphyn.editor.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class GraphynDsTypography(
    val appTitle: TextStyle,
    val panelTitle: TextStyle,
    val nodeTitle: TextStyle,
    val nodeSubtitle: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
) {
    companion object {
        val Default = GraphynDsTypography(
            appTitle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = (-0.2).sp,
            ),
            panelTitle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            ),
            nodeTitle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = (-0.1).sp,
            ),
            nodeSubtitle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
            ),
            label = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
            ),
            body = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            ),
            caption = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 0.2.sp,
            ),
            mono = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            ),
        )
    }
}

val LocalGraphynDsTypography = compositionLocalOf { GraphynDsTypography.Default }
