package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityThemesBinding
import com.mindease.mindeaseapp.utils.ThemeManager
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.google.android.material.R as MaterialR

/**
 * Activity untuk halaman Themes/Tema.
 * Mengelola pilihan Tema Penuh.
 */
class ThemesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesBinding

    private val themeViews by lazy {
        mapOf(
            "light" to binding.tvThemeLight,
            "dark" to binding.tvThemeDark,
            "greentea" to binding.tvColorGreentea,
            "oceanblue" to binding.tvColorOceanblue,
            "rosepink" to binding.tvColorRosepink,
            "autumngold" to binding.tvColorAutumngold
        )
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeStyleResId = ThemeManager.getThemeStyleResId(this)
        setTheme(themeStyleResId)

        super.onCreate(savedInstanceState)

        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupListeners()
        highlightCurrentChoices()
    }

    // ... (Fungsi setupListeners dan setFullTheme tidak berubah)

    private fun setupListeners() {
        themeViews.forEach { (key, view) ->
            view.setOnClickListener {
                setFullTheme(key)
            }
        }
    }

    private fun setFullTheme(key: String) {
        ThemeManager.saveTheme(this, key)
        ThemeManager.applyTheme(this)

        val themeNameResId = when (key) {
            "light" -> R.string.theme_light
            "dark" -> R.string.theme_dark
            "greentea" -> R.string.theme_greentea
            "oceanblue" -> R.string.theme_oceanblue
            "rosepink" -> R.string.theme_rosepink
            "autumngold" -> R.string.theme_autumngold
            else -> R.string.theme_light
        }
        val themeName = getString(themeNameResId)

        Toast.makeText(this, getString(R.string.theme_changed_to_name, themeName), Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    private fun highlightCurrentChoices() {
        val currentThemeKey = ThemeManager.getThemeKey(this)

        themeViews.forEach { (key, view) ->
            if (key == currentThemeKey) {
                applyHighlight(view)
            } else {
                resetHighlight(view)
            }
        }
    }

    /**
     * FIX UTAMA: Menggunakan Drawable yang Bulat untuk Highlight.
     */
    private fun applyHighlight(view: TextView) {
        // Ambil warna untuk teks (OnPrimary)
        val selectedTextColor = resolveThemeColor(this, MaterialR.attr.colorOnPrimary)

        // Gunakan drawable baru untuk background (memastikan sudut tetap 8dp)
        view.setBackgroundResource(R.drawable.bg_theme_selected_highlight)
        view.setTextColor(selectedTextColor)
    }

    private fun resetHighlight(view: TextView) {
        val defaultTextColor = resolveThemeColor(this, MaterialR.attr.colorOnSurface)

        // Gunakan drawable standar (yang sekarang sudah tanpa garis stroke/hitam)
        view.setBackgroundResource(R.drawable.bg_rounded_border)
        view.setTextColor(defaultTextColor)
    }

    private fun resolveThemeColor(context: Context, attr: Int): Int {
        val typedValue = android.util.TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}