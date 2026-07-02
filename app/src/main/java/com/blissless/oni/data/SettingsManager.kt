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
        private const val DEFAULT_SYNC_THRESHOLD = 90
    }

    fun getAniListSyncThreshold(): Int {
        return prefs.getInt(KEY_SYNC_THRESHOLD, DEFAULT_SYNC_THRESHOLD)
    }

    fun setAniListSyncThreshold(percent: Int) {
        prefs.edit().putInt(KEY_SYNC_THRESHOLD, percent.coerceIn(75, 100)).apply()
    }

    fun getCheckUpdatesOnStart(): Boolean {
        return prefs.getBoolean(KEY_CHECK_UPDATES, false)
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
}
