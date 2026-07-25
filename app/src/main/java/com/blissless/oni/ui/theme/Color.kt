package com.blissless.oni.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Light Theme ───────────────────────────────────────────────────────

val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F0)
val LightElevated = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)

// ─── Dark Theme ────────────────────────────────────────────────────────

val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF141414)
val DarkSurfaceVariant = Color(0xFF1E1E1E)
val DarkElevated = Color(0xFF282828)
val DarkCard = Color(0xFF1A1A1E)

// ─── Accent Colors ─────────────────────────────────────────────────────

val Silver = Color(0xFFE5E7EB)
val SilverLight = Color(0xFFF3F4F6)
val SilverDark = Color(0xFF9CA3AF)
val BlueAccent = Color(0xFF3B82F6)
val BlueLight = Color(0xFF60A5FA)
val BlueDark = Color(0xFF2563EB)
val BlueGlow = Color(0xFF93C5FD)

// ─── Status Colors ─────────────────────────────────────────────────────

val StatusWatching = Color(0xFF60A5FA)
val StatusPlanning = Color(0xFFC084FC)
val StatusCompleted = Color(0xFF34D399)
val StatusPaused = Color(0xFFFBBF24)
val StatusDropped = Color(0xFFF87171)

val StatusColors = mapOf(
    "CURRENT" to StatusWatching,
    "READING" to StatusWatching,
    "PLANNING" to StatusPlanning,
    "COMPLETED" to StatusCompleted,
    "PAUSED" to StatusPaused,
    "ON_HOLD" to StatusPaused,
    "DROPPED" to StatusDropped
)

// ─── Glass / Gradient ──────────────────────────────────────────────────

val GlassWhite = Color(0x0DFFFFFF)
val GlassStroke = Color(0x1AFFFFFF)
val GlassStrokeFocused = Color(0x33FFFFFF)

val GradientBlue = Color(0xFF3B82F6)
val GradientPurple = Color(0xFF8B5CF6)
val GradientTeal = Color(0xFF06B6D4)

// ─── Reader ────────────────────────────────────────────────────────────

val ReadGreen = Color(0xFF10B981)
val ReadGreenDim = Color(0xFF065F46)
val UnreadGray = Color(0xFF4B5563)
val CurrentBlueGlow = Color(0x333B82F6)

val ChapterCounterBg = Color(0xFF1E293B)
val SearchBarBg = Color(0xFF1C1C1E)
val ProgressTrackBg = Color(0xFF2A2A2E)

// ─── OLED ──────────────────────────────────────────────────────────────

val OledBlack = Color(0xFF000000)
