package com.blissless.oni.data

import android.content.Context
import android.content.SharedPreferences

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

    /**
     * Reader layout mode:
     *  - "vertical"      : webtoon-style continuous vertical scroll (default, original behaviour)
     *  - "left_to_right" : one page per screen, swipe left → next page (Mihon default for LTR languages)
     *  - "right_to_left" : one page per screen, swipe right → next page (Mihon default for manga / RTL)
     *
     * The string is stored as a small ordinal-free identifier so future versions
     * can rename or reorder the enum without breaking existing installs.
     */
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
        return prefs.getBoolean(KEY_MATERIAL3_COLOR, false)
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
}

/**
 * The three reading layouts supported by the reader screen.
 *
 * - VERTICAL_SCROLL: continuous webtoon-style scrolling, one column of images
 *   stacked vertically. The "classic" Oni behaviour.
 *
 * - LEFT_TO_RIGHT: one page per screen, HorizontalPager laid out so swiping
 *   leftward reveals the next page. Natural for western comics and for users
 *   who think "swipe forward = swipe left".
 *
 * - RIGHT_TO_LEFT: one page per screen, HorizontalPager laid out so swiping
 *   rightward reveals the next page. This is the manga-traditional direction
 *   and matches Mihon's default for Japanese-language manga.
 */
enum class ReaderMode(val storageKey: String, val displayLabel: String) {
    VERTICAL_SCROLL("vertical", "Vertical Scroll"),
    LEFT_TO_RIGHT("ltr", "Left to Right"),
    RIGHT_TO_LEFT("rtl", "Right to Left");
}
