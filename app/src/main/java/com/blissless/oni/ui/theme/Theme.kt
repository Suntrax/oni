package com.blissless.oni.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    OLED("oled");

    companion object {
        fun fromValue(value: String): ThemeMode =
            entries.find { it.value == value } ?: SYSTEM
    }
}

// ─── Dark Schemes ──────────────────────────────────────────────────────

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

private val MonochromeDarkColorScheme = darkColorScheme(
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

private val OledDarkColorScheme = darkColorScheme(
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
    background = OledBlack,
    onBackground = SilverLight,
    surface = OledBlack,
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

private val MonochromeOledColorScheme = darkColorScheme(
    primary = Color(0xFFE8E8E8),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFE8E8E8),
    secondary = Color(0xFFBBBBBB),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF141414),
    onTertiaryContainer = Color(0xFFE8E8E8),
    background = OledBlack,
    onBackground = Color(0xFFE8E8E8),
    surface = OledBlack,
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = OledBlack,
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF242424),
    error = Color(0xFF9E9E9E),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF141414),
    onErrorContainer = Color(0xFFE8E8E8)
)

// ─── Light Schemes ─────────────────────────────────────────────────────

private val OniLightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    primaryContainer = BlueLight.copy(alpha = 0.15f),
    onPrimaryContainer = BlueDark,
    secondary = Color(0xFF6B7280),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF1F2937),
    tertiary = BlueDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = BlueDark,
    background = LightBackground,
    onBackground = Color(0xFF111827),
    surface = LightSurface,
    onSurface = Color(0xFF111827),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainerLow = LightCard,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE5E7EB),
    surfaceContainerHighest = Color(0xFFD1D5DB),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = StatusDropped,
    onError = Color.White,
    errorContainer = StatusDropped.copy(alpha = 0.12f),
    onErrorContainer = StatusDropped
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF1C1C1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4D4D4),
    onPrimaryContainer = Color(0xFF1C1C1C),
    secondary = Color(0xFF3B3B3B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color(0xFF1C1C1C),
    tertiary = Color(0xFF595959),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF1C1C1C),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1C1C),
    surface = LightSurface,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF3B3B3B),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFFD4D4D4),
    error = Color(0xFF595959),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF0F0F0),
    onErrorContainer = Color(0xFF1C1C1C)
)

// ─── App Theme ─────────────────────────────────────────────────────────

@Composable
fun OniTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    useMonochrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }

    val colorScheme = when {
        useMonochrome && themeMode == ThemeMode.OLED -> MonochromeOledColorScheme
        useMonochrome && darkTheme -> MonochromeDarkColorScheme
        useMonochrome -> MonochromeLightColorScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OniDarkColorScheme
        else -> OniLightColorScheme
    }.let { scheme ->
        if (themeMode == ThemeMode.OLED && !useMonochrome) {
            scheme.copy(
                surface = OledBlack,
                background = OledBlack,
                surfaceVariant = OledBlack,
                primaryContainer = scheme.primaryContainer.copy(alpha = 0.2f)
            )
        } else scheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
