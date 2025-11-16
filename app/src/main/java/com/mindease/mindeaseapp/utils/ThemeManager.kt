package com.mindease.mindeaseapp.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.R.style

data class ThemePalette(
    val key: String,
    val name: String,
    val styleResId: Int // Resource ID dari Style (R.style.Theme_MindEase_XYZ)
)

/**
 * Utility class untuk mengelola preferensi tema penuh (Style Resource)
 * menggunakan Shared Preferences, menghilangkan System Default.
 */
object ThemeManager {
    // Shared Preferences Keys
    private const val PREFS_NAME = "theme_prefs"
    private const val THEME_KEY = "user_theme_key"

    // Default: FORCE MindEase Indigo (LIGHT)
    const val DEFAULT_THEME = "INDIGO" // Default ke Light Theme (Indigo)

    // --- Daftar 6 Pilihan Tema Penuh (Semua Sejajar) ---
    val AVAILABLE_THEMES = listOf(
        // Tema Utama
        ThemePalette("INDIGO", "MindEase Light (Indigo)", style.Theme_MindEase_Indigo),
        ThemePalette("DARK_MODE", "MindEase Dark", style.Theme_MindEase_Dark),
        // Tema Kustom
        ThemePalette("GREENTEA", "Green Tea", style.Theme_MindEase_GreenTea),
        ThemePalette("OCEANBLUE", "Ocean Blue", style.Theme_MindEase_OceanBlue),
        ThemePalette("ROSEPINK", "Rose Pink", style.Theme_MindEase_RosePink),
        ThemePalette("AUTUMNGOLD", "Autumn Gold", style.Theme_MindEase_AutumnGold)
    )

    // --- Logika Penyimpanan & Pengambilan ---

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Menyimpan pilihan tema penuh dan mengatur Night Mode untuk MENCEGAH override sistem.
     */
    fun saveTheme(context: Context, key: String) {
        getPrefs(context).edit().putString(THEME_KEY, key).apply()

        // Blok Dark Mode System Override:
        // Set mode yang sesuai: YES untuk Dark Mode, NO untuk semua tema Light/Kustom lainnya
        val mode = if (key == "DARK_MODE") {
            AppCompatDelegate.MODE_NIGHT_YES // Aktifkan Dark Mode untuk tema Dark
        } else {
            AppCompatDelegate.MODE_NIGHT_NO  // Paksa Light Mode untuk semua tema Light/Kustom
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Mengambil Resource ID Style yang benar untuk diterapkan.
     */
    fun getThemeStyleResId(context: Context): Int {
        val key = getThemeKey(context)

        return AVAILABLE_THEMES.find { it.key == key }?.styleResId
            ?: AVAILABLE_THEMES.first().styleResId
    }

    /**
     * Mengambil kunci tema penuh yang tersimpan.
     */
    fun getThemeKey(context: Context): String {
        // Default selalu ke Indigo Light jika belum pernah memilih
        return getPrefs(context).getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
    }
}