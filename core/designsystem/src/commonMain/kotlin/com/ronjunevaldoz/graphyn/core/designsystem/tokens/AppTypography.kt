package com.ronjunevaldoz.graphyn.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val displayLarge: TextStyle  = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 36.sp, fontWeight = FontWeight.Bold,     lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    val displayMedium: TextStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp, fontWeight = FontWeight.Bold,     lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    val titleLarge: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    val titleMedium: TextStyle   = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    val titleSmall: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    val bodyLarge: TextStyle     = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.Normal,   lineHeight = 24.sp),
    val bodyMedium: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp),
    val bodySmall: TextStyle     = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.Normal,   lineHeight = 16.sp),
    val labelLarge: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Medium,   lineHeight = 20.sp, letterSpacing = 0.1.sp),
    val labelSmall: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, fontWeight = FontWeight.Medium,   lineHeight = 16.sp, letterSpacing = 0.5.sp),
    val mono: TextStyle          = TextStyle(fontFamily = FontFamily.Monospace,  fontSize = 13.sp, fontWeight = FontWeight.Normal,   lineHeight = 20.sp),
    /** Compact label used inside canvas node cards — port names, field labels. */
    val nodeLabel: TextStyle     = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, fontWeight = FontWeight.Normal,   lineHeight = 14.sp),
    /** Header label used inside canvas node cards — node title row. */
    val nodeHeader: TextStyle    = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
)
