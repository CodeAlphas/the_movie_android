package com.codealphas.themovie.core.android.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 남은 XML 화면이 같은 색을 써야 하므로, 값은 :presentation의 values/colors.xml과 values-night/colors.xml에 맞춰 적용
private val LightPrimary = Color(0xFF1A1916)
private val LightOnPrimary = Color(0xFFF3F0EA)
private val LightSecondary = Color(0xFFA34B2E)
private val LightOnSecondary = Color(0xFFFBF7F2)
private val LightTertiary = Color(0xFFA34B2E)
private val LightTertiaryContainer = Color(0xFFE4C2A4)
private val LightOnTertiaryContainer = Color(0xFF1A1916)
private val LightSurface = Color(0xFFF3F0EA)
private val LightOnSurface = Color(0xFF1A1916)
private val LightSurfaceRaised = Color(0xFFFFFCFA)
private val LightSurfaceVariant = Color(0xFFE9E4DB)
private val LightOnSurfaceVariant = Color(0xFF6E695F)
private val LightOutline = Color(0xFFA89886)

private val DarkPrimary = Color(0xFFE8E2D6)
private val DarkOnPrimary = Color(0xFF1A1916)
private val DarkSecondary = Color(0xFFC46A45)
private val DarkOnSecondary = Color(0xFF1A100C)
private val DarkTertiary = Color(0xFFE0A080)
private val DarkTertiaryContainer = Color(0xFF4A382C)
private val DarkOnTertiaryContainer = Color(0xFFF3EEE6)
private val DarkSurface = Color(0xFF0F0E0C)
private val DarkOnSurface = Color(0xFFF3EEE6)
private val DarkSurfaceRaised = Color(0xFF1A1916)
private val DarkSurfaceVariant = Color(0xFF1A1916)
private val DarkOnSurfaceVariant = Color(0xFFA39E94)
private val DarkOutline = Color(0xFF8A8174)

// surfaceContainer 계열을 비워 두면 DropdownMenu, Card, ModalBottomSheet가 Material3 기본 보라 회색이 되므로,
// 브랜드 표면색 세 가지로 모든 단계 적용
internal val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightSurfaceVariant,
        onPrimaryContainer = LightOnSurface,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondary,
        onSecondaryContainer = LightOnSecondary,
        tertiary = LightTertiary,
        tertiaryContainer = LightTertiaryContainer,
        onTertiaryContainer = LightOnTertiaryContainer,
        background = LightSurface,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        surfaceContainerLowest = LightSurface,
        surfaceContainerLow = LightSurface,
        surfaceContainer = LightSurfaceRaised,
        surfaceContainerHigh = LightSurfaceRaised,
        surfaceContainerHighest = LightSurfaceVariant,
    )

internal val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkSurfaceVariant,
        onPrimaryContainer = DarkOnSurface,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondary,
        onSecondaryContainer = DarkOnSecondary,
        tertiary = DarkTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = DarkSurface,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        surfaceContainerLowest = DarkSurface,
        surfaceContainerLow = DarkSurface,
        surfaceContainer = DarkSurfaceRaised,
        surfaceContainerHigh = DarkSurfaceRaised,
        surfaceContainerHighest = DarkSurfaceVariant,
    )
