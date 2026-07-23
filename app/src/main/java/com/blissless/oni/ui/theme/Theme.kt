package com.blissless.oni.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = SilverDark.copy(alpha = 0.3f),
    outlineVariant = DarkElevated,
    error = StatusDropped,
    onError = Color.White,
    errorContainer = StatusDropped.copy(alpha = 0.2f),
    onErrorContainer = StatusDropped
)

private val MonochromeDarkScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFBDBDBD),
    onPrimaryContainer = Color(0xFF212121),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF424242),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF616161),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceContainerLow = Color(0xFF1A1A1E),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFFBABA)
)

private val OledDarkScheme = darkColorScheme(
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
    background = Color.Black,
    onBackground = SilverLight,
    surface = Color.Black,
    onSurface = SilverLight,
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = SilverDark,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    outline = SilverDark.copy(alpha = 0.3f),
    outlineVariant = Color(0xFF141414),
    error = StatusDropped,
    onError = Color.White,
    errorContainer = StatusDropped.copy(alpha = 0.2f),
    onErrorContainer = StatusDropped
)

@Composable
fun OniTheme(
    darkTheme: Boolean = true,
    useMaterial3Color: Boolean = false,
    monochromeTheme: Boolean = false,
    oledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useMaterial3Color && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        monochromeTheme -> MonochromeDarkScheme
        oledTheme -> OledDarkScheme
        else -> OniDarkColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
