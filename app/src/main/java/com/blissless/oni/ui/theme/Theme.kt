package com.blissless.oni.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OniDarkColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    primaryContainer = BlueDark,
    onPrimaryContainer = BlueGlow,
    secondary = Silver,
    onSecondary = Color.Black,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = SilverLight,
    tertiary = BlueLight,
    onTertiary = Color.Black,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = Silver,
    background = DarkBackground,
    onBackground = SilverLight,
    surface = DarkSurface,
    onSurface = SilverLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SilverDark,
    outline = SilverDark.copy(alpha = 0.3f),
    outlineVariant = DarkElevated,
    error = StatusDropped,
    onError = Color.White,
    errorContainer = StatusDropped.copy(alpha = 0.2f),
    onErrorContainer = StatusDropped
)

@Composable
fun OniTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = OniDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.hashCode()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
