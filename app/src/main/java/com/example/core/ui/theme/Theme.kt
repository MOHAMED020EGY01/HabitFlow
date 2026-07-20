package com.example.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

// ── Dark Mode color scheme — UNCHANGED byte-for-byte from Part 1 ────────
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    error = ErrorColor
)

// ── Light Mode color scheme — updated Part 91 ───────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = OnPrimaryLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    error = ErrorColor
)

// Light Mode gradient brush for screen-level backgrounds
val LightBackgroundGradientBrush = Brush.verticalGradient(
    colors = listOf(LightBackgroundGradientTop, LightBackgroundGradientBottom)
)

@Composable
fun HabitFlowTheme(
    darkTheme: Boolean = true, // Forced dark mode
    content: @Composable () -> Unit
) {
    // Force Dark Mode — Task 4 fix
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
