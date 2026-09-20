package com.zamnimeku.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = WibukuPrimary,
    onPrimary = WibukuSurface,
    primaryContainer = WibukuBadge,
    onPrimaryContainer = WibukuPrimary,
    secondary = WibukuAccent,
    onSecondary = WibukuSurface,
    background = WibukuBg,
    onBackground = WibukuText,
    surface = WibukuSurface,
    onSurface = WibukuText,
    surfaceVariant = WibukuBg,
    outline = WibukuBorder
)

@Composable
fun ZamnimekuTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WibukuBg.toArgb()
            window.navigationBarColor = WibukuSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
