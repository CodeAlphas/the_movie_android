package com.codealphas.themovie.core.android.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 그라데이션 끝색과 반투명 강조색은 Material3 ColorScheme에 대응하는 역할이 없으므로, 테마 밖 별도 색으로 제공
@Immutable
data class ExtendedColors(
    val primaryGradientStart: Color,
    val primaryGradientEnd: Color,
    val tertiaryContainerEnd: Color,
    val onPrimaryScrim: Color,
)

internal val LightExtendedColors =
    ExtendedColors(
        primaryGradientStart = Color(0xFFCDB8A2),
        primaryGradientEnd = Color(0xFFB08968),
        tertiaryContainerEnd = Color(0xFFC9926A),
        onPrimaryScrim = Color(0x33A34B2E),
    )

internal val DarkExtendedColors =
    ExtendedColors(
        primaryGradientStart = Color(0xFF3A3128),
        primaryGradientEnd = Color(0xFF4A3C30),
        tertiaryContainerEnd = Color(0xFF342820),
        onPrimaryScrim = Color(0x40E0A080),
    )

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
