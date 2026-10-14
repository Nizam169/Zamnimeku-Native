package com.zamnimeku.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AnimeLogColorScheme = darkColorScheme(
    primary = WibukuPrimary,
    onPrimary = WibukuText,
    primaryContainer = WibukuBadge,
    onPrimaryContainer = WibukuPrimary,
    secondary = WibukuAccent,
    onSecondary = WibukuBg,
    background = WibukuBg,
    onBackground = WibukuText,
    surface = WibukuSurface,
    onSurface = WibukuText,
    surfaceVariant = WibukuCard,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = AnimeLogColorScheme,
        typography = AppTypography,
        content = content
    )
}
