package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AfhDarkColorScheme = darkColorScheme(
    primary = AfhElectricBlue,
    onPrimary = AfhWhite,
    primaryContainer = AfhElevatedNavy,
    onPrimaryContainer = AfhCyan,
    secondary = AfhCyan,
    onSecondary = AfhDeepNavy,
    secondaryContainer = Color(0xFF0E2C47),
    onSecondaryContainer = AfhCyan,
    tertiary = AfhSoftPurple,
    onTertiary = AfhWhite,
    tertiaryContainer = Color(0xFF251A4D),
    onTertiaryContainer = Color(0xFFD6C7FF),
    background = AfhDeepNavy,
    onBackground = AfhWhite,
    surface = AfhDarkBlue,
    onSurface = AfhWhite,
    surfaceVariant = AfhElevatedNavy,
    onSurfaceVariant = AfhMutedSlate,
    outline = AfhSurfaceBorderDark,
    outlineVariant = Color(0xFF1A2948),
    error = AfhError,
    onError = AfhWhite
)

private val AfhLightColorScheme = lightColorScheme(
    primary = AfhElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF09367A),
    secondary = Color(0xFF0099BA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F7FF),
    onSecondaryContainer = Color(0xFF004C5E),
    tertiary = AfhSoftPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE6FF),
    onTertiaryContainer = Color(0xFF391C8A),
    background = AfhLightBackground,
    onBackground = AfhLightOnSurface,
    surface = AfhLightSurface,
    onSurface = AfhLightOnSurface,
    surfaceVariant = AfhLightSurfaceVariant,
    onSurfaceVariant = AfhLightOnSurfaceVariant,
    outline = AfhLightBorder,
    outlineVariant = Color(0xFFE3ECFA),
    error = Color(0xFFD92D53),
    onError = Color.White
)

val AfhShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AfhAiTheme(
    themeMode: String = "dark",
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val colorScheme = if (useDark) AfhDarkColorScheme else AfhLightColorScheme
    val typography = buildAfhTypography(isArabic)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AfhShapes,
        content = content
    )
}
