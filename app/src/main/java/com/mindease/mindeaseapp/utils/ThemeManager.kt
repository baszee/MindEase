package com.mindease.mindeaseapp.utils // ✅ Package sudah BENAR

import android.content.Context
import android.os.Build
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mindease.mindeaseapp.R // ✅ Import R sudah BENAR
import java.util.Locale

/**
 * Utility Object untuk mengelola tema (theme) dan bahasa (locale) aplikasi secara persisten.
 */
object ThemeManager {
    // --- Konstanta ---
    const val PREFS_NAME = "AppPrefs"
    const val THEME_KEY = "theme_style"
    const val LANGUAGE_KEY = "language_code"
    const val DEFAULT_THEME = "light"
    const val DEFAULT_LANGUAGE = "en"

    // --- Utility ---
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Pemrosesan Tema ---

    fun getThemeKey(context: Context): String {
        return getPrefs(context).getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    /**
     * ✅ FIX: Menggunakan nama style yang benar dari themes.xml Anda.
     */
    fun getThemeStyleResId(context: Context): Int {
        // Berdasarkan file themes.xml yang Anda berikan, ini adalah nama style-nya
        return when (getThemeKey(context)) {
            "light" -> R.style.Theme_MindEase_Indigo
            "dark" -> R.style.Theme_MindEase_Dark
            "greentea" -> R.style.Theme_MindEase_GreenTea
            "oceanblue" -> R.style.Theme_MindEase_OceanBlue
            "rosepink" -> R.style.Theme_MindEase_RosePink
            "autumngold" -> R.style.Theme_MindEase_AutumnGold
            else -> R.style.Theme_MindEase_Indigo // Fallback
        }
    }

    fun applyTheme(context: Context) {
        val themeKey = getThemeKey(context)
        val mode = when (themeKey) {
            "light", "greentea", "oceanblue", "rosepink", "autumngold" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun saveTheme(context: Context, themeKey: String) {
        getPrefs(context).edit().putString(THEME_KEY, themeKey).apply()
    }

    // --- Pemrosesan Bahasa (Locale) ---

    fun getLanguageCode(context: Context): String {
        return getPrefs(context).getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun saveLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    /**
     * Menerapkan Locale baru ke Context. Dipanggil di setiap Activity.attachBaseContext().
     */
    fun wrapContext(context: Context): Context {
        val languageCode = getLanguageCode(context)
        val locale = Locale.Builder().setLanguage(languageCode).build()

        // 1. Set locale default untuk Java/Kotlin APIs
        Locale.setDefault(locale)

        val config: Configuration = context.resources.configuration

        // 2. Set locale pada Configuration (Cara modern dan deprecated handling)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        // 3. Kembalikan Context baru dengan Configuration yang diperbarui (Kunci agar bahasa bekerja)
        return context.createConfigurationContext(config)
    }
}