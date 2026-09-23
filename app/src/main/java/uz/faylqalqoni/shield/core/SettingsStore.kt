package uz.faylqalqoni.shield.core

import android.content.Context

/**
 * Ilova sozlamalarini (hozircha — Google Safe Browsing API kaliti) saqlash uchun
 * oddiy SharedPreferences o'ramasi.
 */
object SettingsStore {
    private const val PREFS_NAME = "faylqalqoni_settings"
    private const val KEY_SAFE_BROWSING_API_KEY = "safe_browsing_api_key"

    fun getSafeBrowsingApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAFE_BROWSING_API_KEY, "") ?: ""
    }

    fun setSafeBrowsingApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAFE_BROWSING_API_KEY, apiKey.trim()).apply()
    }
}
