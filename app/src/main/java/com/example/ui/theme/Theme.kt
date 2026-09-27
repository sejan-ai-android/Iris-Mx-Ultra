package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IrisDarkColorScheme = darkColorScheme(
    primary = IrisCyanPrimary,
    onPrimary = IrisBackground,
    primaryContainer = IrisSurfaceVariant,
    onPrimaryContainer = IrisCyanPrimary,
    secondary = IrisVioletSecondary,
    onSecondary = IrisBackground,
    secondaryContainer = IrisSurfaceElevated,
    onSecondaryContainer = IrisVioletSecondary,
    tertiary = IrisEmeraldAccent,
    background = IrisBackground,
    onBackground = IrisTextPrimary,
    surface = IrisSurface,
    onSurface = IrisTextPrimary,
    surfaceVariant = IrisSurfaceVariant,
    onSurfaceVariant = IrisTextSecondary,
    error = IrisAlertRed,
    onError = IrisTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force high-tech dark neural aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = IrisDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = IrisBackground.toArgb()
                window.navigationBarColor = IrisBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
