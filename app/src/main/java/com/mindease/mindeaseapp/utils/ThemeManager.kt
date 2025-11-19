package com.mindease.mindeaseapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mindease.mindeaseapp.R
import java.util.Locale
import kotlin.system.exitProcess

object ThemeManager {
    const val PREFS_NAME = "AppPrefs"
    const val THEME_KEY = "theme_style"
    const val LANGUAGE_KEY = "language_code"
    const val DEFAULT_THEME = "light"
    const val DEFAULT_LANGUAGE = "en"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeKey(context: Context): String {
        return getPrefs(context).getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun getThemeStyleResId(context: Context): Int {
        return when (getThemeKey(context)) {
            "light" -> R.style.Theme_MindEase_Indigo
            "dark" -> R.style.Theme_MindEase_Dark
            "greentea" -> R.style.Theme_MindEase_GreenTea
            "oceanblue" -> R.style.Theme_MindEase_OceanBlue
            "rosepink" -> R.style.Theme_MindEase_RosePink
            "autumngold" -> R.style.Theme_MindEase_AutumnGold
            else -> R.style.Theme_MindEase_Indigo
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

    fun getLanguageCode(context: Context): String {
        return getPrefs(context).getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun saveLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    /**
     * ✅ FIX UTAMA: Mapping "id" ke "in" untuk Android
     */
    fun wrapContext(context: Context): Context {
        val languageCode = getLanguageCode(context)

        // Android menggunakan "in" untuk Indonesia, bukan "id"
        val androidLanguageCode = if (languageCode == "id") "in" else languageCode

        android.util.Log.d("ThemeManager", "Applying language: $languageCode (Android: $androidLanguageCode)")

        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Locale.forLanguageTag(androidLanguageCode)
        } else {
            Locale(androidLanguageCode)
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(android.os.LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    fun restartApplication(activity: Activity) {
        android.util.Log.d("ThemeManager", "Restarting application...")

        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        activity.startActivity(intent)
        activity.finish()

        exitProcess(0)
    }
}