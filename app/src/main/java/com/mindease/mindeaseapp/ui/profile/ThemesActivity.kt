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
import com.mindease.mindeaseapp.ui.home.MainActivity // Import MainActivity
import com.google.android.material.R as MaterialR

/**
 * Activity untuk halaman Themes/Tema.
 * Mengelola pilihan Tema Penuh.
 */
class ThemesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesBinding

    // Daftar TextView untuk semua pilihan tema
    private val themeViews by lazy {
        mapOf(
            "INDIGO" to binding.tvThemeLight,
            "DARK_MODE" to binding.tvThemeDark,
            "GREENTEA" to binding.tvColorGreentea,
            "OCEANBLUE" to binding.tvColorOceanblue,
            "ROSEPINK" to binding.tvColorRosepink,
            "AUTUMNGOLD" to binding.tvColorAutumngold
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Terapkan tema sebelum super.onCreate()
        val themeStyleResId = ThemeManager.getThemeStyleResId(this)
        setTheme(themeStyleResId)

        super.onCreate(savedInstanceState)

        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupListeners()
        highlightCurrentChoices()
    }

    private fun setupListeners() {
        themeViews.forEach { (key, view) ->
            view.setOnClickListener {
                setFullTheme(key)
            }
        }
    }

    private fun setFullTheme(key: String) {
        ThemeManager.saveTheme(this, key) // Simpan pilihan dan atur Night Mode

        val toastMessage = when (key) {
            "INDIGO" -> "Tema diubah ke MindEase Light."
            "DARK_MODE" -> "Tema diubah ke MindEase Dark."
            else -> "Tema diubah ke $key."
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()

        // 🔥 FIX UTAMA PROPAGATION: Restart aplikasi dari MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            // Hapus semua Activity yang ada di stack dan mulai MainActivity sebagai root baru
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        // PENTING: Tutup Activity Themes agar tidak kembali ke sini
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

    private fun applyHighlight(view: TextView) {
        val selectedBgColor = resolveThemeColor(this, MaterialR.attr.colorPrimary)
        val selectedTextColor = resolveThemeColor(this, MaterialR.attr.colorOnPrimary)

        view.setBackgroundColor(selectedBgColor)
        view.setTextColor(selectedTextColor)
    }

    private fun resetHighlight(view: TextView) {
        val defaultTextColor = resolveThemeColor(this, MaterialR.attr.colorOnSurface)

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