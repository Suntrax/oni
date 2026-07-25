package com.blissless.oni.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "Oni_settings"
        private const val KEY_SYNC_THRESHOLD = "anilist_sync_threshold"
        private const val KEY_CHECK_UPDATES = "check_updates_on_start"
        private const val KEY_SELECTED_EXTENSION = "selected_extension_authority"
        private const val KEY_READER_MODE = "reader_mode"
        private const val KEY_LOCK_READER_ROTATION = "lock_reader_rotation"
        private const val KEY_MATERIAL3_COLOR = "material3_color"
        private const val KEY_MONOCHROME_THEME = "monochrome_theme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SHOW_PAGE_INDICATOR = "show_page_indicator"
        private const val KEY_STARTUP_SCREEN = "startup_screen"
        private const val DEFAULT_SYNC_THRESHOLD = 90
    }

    fun getAniListSyncThreshold(): Int {
        return prefs.getInt(KEY_SYNC_THRESHOLD, DEFAULT_SYNC_THRESHOLD)
    }

    fun setAniListSyncThreshold(percent: Int) {
        prefs.edit().putInt(KEY_SYNC_THRESHOLD, percent.coerceIn(75, 100)).apply()
    }

    fun getCheckUpdatesOnStart(): Boolean {
        return prefs.getBoolean(KEY_CHECK_UPDATES, true)
    }

    fun setCheckUpdatesOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHECK_UPDATES, enabled).apply()
    }

    fun getSelectedExtensionAuthority(): String? {
        return prefs.getString(KEY_SELECTED_EXTENSION, null)
    }

    fun setSelectedExtensionAuthority(authority: String?) {
        prefs.edit().putString(KEY_SELECTED_EXTENSION, authority).apply()
    }

    fun getReaderMode(): ReaderMode {
        return when (prefs.getString(KEY_READER_MODE, ReaderMode.VERTICAL_SCROLL.storageKey)) {
            ReaderMode.LEFT_TO_RIGHT.storageKey -> ReaderMode.LEFT_TO_RIGHT
            ReaderMode.RIGHT_TO_LEFT.storageKey -> ReaderMode.RIGHT_TO_LEFT
            else -> ReaderMode.VERTICAL_SCROLL
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        prefs.edit().putString(KEY_READER_MODE, mode.storageKey).apply()
    }

    fun getLockReaderRotation(): Boolean {
        return prefs.getBoolean(KEY_LOCK_READER_ROTATION, true)
    }

    fun setLockReaderRotation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_READER_ROTATION, enabled).apply()
    }

    fun getMaterial3Color(): Boolean {
        return prefs.getBoolean(KEY_MATERIAL3_COLOR, true)
    }

    fun setMaterial3Color(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MATERIAL3_COLOR, enabled).apply()
    }

    fun getMonochromeTheme(): Boolean {
        return prefs.getBoolean(KEY_MONOCHROME_THEME, false)
    }

    fun setMonochromeTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONOCHROME_THEME, enabled).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getShowPageIndicator(): Boolean {
        return prefs.getBoolean(KEY_SHOW_PAGE_INDICATOR, true)
    }

    fun setShowPageIndicator(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PAGE_INDICATOR, enabled).apply()
    }

    fun getStartupScreen(): String {
        return prefs.getString(KEY_STARTUP_SCREEN, "home") ?: "home"
    }

    fun setStartupScreen(screen: String) {
        prefs.edit().putString(KEY_STARTUP_SCREEN, screen).apply()
    }
}

enum class ReaderMode(
    val storageKey: String,
    val displayLabel: String,
    val description: String,
    val icon: ImageVector
) {
    VERTICAL_SCROLL("vertical", "Vertical Scroll", "Webtoon-style continuous scroll", Icons.Default.ViewAgenda),
    LEFT_TO_RIGHT("ltr", "Left to Right", "One page per screen, swipe left", Icons.AutoMirrored.Filled.ArrowForward),
    RIGHT_TO_LEFT("rtl", "Right to Left", "One page per screen, swipe right (manga)", Icons.AutoMirrored.Filled.ArrowBack);
}
