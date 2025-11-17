package com.mindease.mindeaseapp.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.mindease.mindeaseapp.R
import java.util.Locale
import com.mindease.mindeaseapp.R.style

/**
 * Model untuk menyimpan semua Local Preferences
 */
data class AppPreferences(
    val themeKey: String,
    val language: String,
    val notificationEnabled: Boolean,
    val soundEnabled: Boolean
)

/**
 * Utility class untuk mengelola preferensi lokal (Tema, Bahasa, Notifikasi) menggunakan Shared Preferences.
 */
object ThemeManager {

    private const val PREFS_NAME = "app_local_prefs"
    private const val THEME_KEY = "user_theme_key"
    private const val LANGUAGE_KEY = "user_language_key"
    private const val NOTIFICATION_KEY = "notification_enabled"

    const val DEFAULT_THEME = "INDIGO"
    const val DEFAULT_LANGUAGE = "en"

    val AVAILABLE_THEMES = listOf(
        ThemePalette("INDIGO", "MindEase Light (Indigo)", style.Theme_MindEase_Indigo),
        ThemePalette("DARK_MODE", "MindEase Dark", style.Theme_MindEase_Dark),
        ThemePalette("GREENTEA", "Green Tea", style.Theme_MindEase_GreenTea),
        ThemePalette("OCEANBLUE", "Ocean Blue", style.Theme_MindEase_OceanBlue),
        ThemePalette("ROSEPINK", "Rose Pink", style.Theme_MindEase_RosePink),
        ThemePalette("AUTUMNGOLD", "Autumn Gold", style.Theme_MindEase_AutumnGold)
    )

    // --- LOGIKA UTAMA (Shared Preferences Access) ---
    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- LOGIKA TEMA ---

    fun saveTheme(context: Context, key: String) {
        getPrefs(context).edit().putString(THEME_KEY, key).apply()
        val mode = if (key == "DARK_MODE") {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getThemeStyleResId(context: Context): Int {
        val key = getThemeKey(context)
        return AVAILABLE_THEMES.find { it.key == key }?.styleResId
            ?: AVAILABLE_THEMES.first().styleResId
    }

    fun getThemeKey(context: Context): String {
        return getPrefs(context).getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    // --- LOGIKA PENGATURAN BARU ---

    fun saveLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    // Fungsi lainnya...
    // ...

    // 🔥 BARU: FUNGSI UNTUK MEMBUNGKUS CONTEXT DENGAN LOKALE YANG DISIMPAN
    fun wrapContext(context: Context): Context {
        val languageCode = getLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        // Menggunakan setLocale (lebih modern dan aman)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}

data class ThemePalette(
    val key: String,
    val name: String,
    val styleResId: Int
)