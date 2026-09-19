package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppleSystemBlueDark,
    onPrimary = AppleLabelPrimaryLight,
    primaryContainer = AppleSystemBlueDark.copy(alpha = 0.2f),
    onPrimaryContainer = AppleSystemBlueDark,
    secondary = AppleSystemOrangeDark,
    secondaryContainer = AppleSystemOrangeDark.copy(alpha = 0.2f),
    onSecondaryContainer = AppleSystemOrangeDark,
    surface = AppleSurfaceDark,
    onSurface = AppleLabelPrimaryDark,
    background = AppleCanvasDark,
    onBackground = AppleLabelPrimaryDark,
    surfaceVariant = AppleSurfaceVariantDark,
    onSurfaceVariant = AppleLabelSecondaryDark,
    outline = AppleSeparatorDark,
    outlineVariant = AppleGlassBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = AppleSystemBlue,
    onPrimary = AppleSurfaceLight,
    primaryContainer = AppleSystemBlue.copy(alpha = 0.12f),
    onPrimaryContainer = AppleSystemBlue,
    secondary = AppleSystemOrange,
    secondaryContainer = AppleSystemOrange.copy(alpha = 0.15f),
    onSecondaryContainer = AppleSystemOrange,
    surface = AppleSurfaceLight,
    onSurface = AppleLabelPrimaryLight,
    background = AppleCanvasLight,
    onBackground = AppleLabelPrimaryLight,
    surfaceVariant = AppleSurfaceVariantLight,
    onSurfaceVariant = AppleLabelSecondaryLight,
    outline = AppleSeparatorLight,
    outlineVariant = AppleGlassBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature Apple OS Human Interface palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
