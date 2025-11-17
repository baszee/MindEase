package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityThemesBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.ThemeManager
import java.util.Locale
import androidx.core.content.ContextCompat
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Activity untuk memilih pengaturan Bahasa.
 */
class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesBinding

    companion object {
        const val LANG_EN = "en"
        const val LANG_ID_ALT = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = getString(R.string.select_language_title) // 🔥 String title baru (asumsi sudah dibuat)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupLanguageOptions()
        highlightCurrentLanguage()
    }

    // 🔥 BARU: Helper untuk mendapatkan warna tema yang konsisten
    @ColorInt
    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return ContextCompat.getColor(this, typedValue.resourceId)
    }


    private fun setupLanguageOptions() {
        // 🔥 FIX 1: Menggunakan strings.xml baru
        binding.tvThemeLight.text = getString(R.string.language_english) // Dulu 'language_english'
        binding.tvThemeDark.text = getString(R.string.language_indonesia) // Dulu 'language_indonesia'

        // 🔥 FIX 2: Menghilangkan teks "Tema Cerah" di header
        // Kita asumsikan TextView yang berisi header itu bernama tv_color_greentea (atau sejenisnya)
        // Dibiarkan saja karena layout yang dipakai adalah ActivityThemesBinding

        binding.tvThemeLight.setOnClickListener {
            setNewLocaleAndRestart(LANG_EN)
        }

        binding.tvThemeDark.setOnClickListener {
            setNewLocaleAndRestart(LANG_ID_ALT)
        }

        // Sembunyikan TextView lain di layout Themes (tetap sama)
        binding.tvColorGreentea.visibility = View.GONE
        binding.tvColorOceanblue.visibility = View.GONE
        binding.tvColorRosepink.visibility = View.GONE
        binding.tvColorAutumngold.visibility = View.GONE
    }

    private fun highlightCurrentLanguage() {
        val currentLang = ThemeManager.getLanguage(this)

        // 🔥 FIX 3: Menggunakan color attribute dari tema/material design
        val primaryColor = resolveThemeColor(com.google.android.material.R.attr.colorPrimary)
        val onSurfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val surfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface) // FIX: Menggunakan colorSurface untuk background tidak terpilih

        // Asumsi: Warna teks yang dipilih harus kontras (colorOnPrimary di themes.xml Anda)
        val selectedTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary)

        // Warna default tombol adalah background tombol itu sendiri (colorSurface)
        val defaultTextColor = onSurfaceColor

        if (currentLang == LANG_EN) {
            // Highlight English
            binding.tvThemeLight.setBackgroundColor(primaryColor)
            binding.tvThemeLight.setTextColor(selectedTextColor)

            // Reset Indonesian
            binding.tvThemeDark.setBackgroundColor(surfaceColor)
            binding.tvThemeDark.setTextColor(defaultTextColor)

        } else if (currentLang == LANG_ID_ALT) {
            // Highlight Indonesian
            binding.tvThemeDark.setBackgroundColor(primaryColor)
            binding.tvThemeDark.setTextColor(selectedTextColor)

            // Reset English
            binding.tvThemeLight.setBackgroundColor(surfaceColor)
            binding.tvThemeLight.setTextColor(defaultTextColor)
        }

        // FIX 4: Menghilangkan teks header tema yang tidak relevan
        // Kita asumsikan TextView yang berisi "PILIHAN TEMA APLIKASI" ada di ActivityThemesBinding
        // Kita cari TV pertama di linear layout dan menyetelnya
        // Karena tidak ada ID untuk teks header, kita abaikan pembersihan UI ini untuk menghindari crash
        // Namun, jika Anda dapat menambahkan ID ke TextView "PILIHAN TEMA APLIKASI" di activity_themes.xml, Anda bisa menyetel visibility-nya ke View.GONE
    }


    private fun setNewLocaleAndRestart(languageCode: String) {
        ThemeManager.saveLanguage(this, languageCode)

        // Restart aplikasi dari Splash Activity untuk memastikan Context diterapkan di awal
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}