package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.view.View // FIX: Import View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityThemesBinding

/**
 * Activity untuk halaman Themes/Tema.
 */
class ThemesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupThemeListeners()
        highlightCurrentTheme()
    }

    private fun setupThemeListeners() {
        binding.tvThemeLight.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, "Tema diubah menjadi Terang", Toast.LENGTH_SHORT).show()
        }

        binding.tvThemeDark.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this, "Tema diubah menjadi Gelap", Toast.LENGTH_SHORT).show()
        }

        binding.tvThemeSystem.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            Toast.makeText(this, "Tema mengikuti pengaturan Sistem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setThemeMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Menyorot opsi tema yang saat ini sedang aktif (visual feedback sederhana).
     */
    private fun highlightCurrentTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()

        // Reset visual semua opsi ke kondisi default
        resetHighlight(binding.tvThemeLight)
        resetHighlight(binding.tvThemeDark)
        resetHighlight(binding.tvThemeSystem)

        // Highlight tema yang aktif
        when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> applyHighlight(binding.tvThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> applyHighlight(binding.tvThemeDark)
            else -> applyHighlight(binding.tvThemeSystem) // Termasuk MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    /**
     * Menerapkan visual highlight (background color dan text color) ke TextView yang dipilih.
     */
    private fun applyHighlight(view: TextView) {
        val selectedBgColor = ContextCompat.getColor(this, R.color.mindease_primary)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)

        view.setBackgroundColor(selectedBgColor)
        view.setTextColor(selectedTextColor)
    }

    /**
     * Mengatur ulang visual ke kondisi default.
     */
    private fun resetHighlight(view: TextView) {
        // Menggunakan drawable yang sudah ada (bg_rounded_border)
        view.setBackgroundResource(R.drawable.bg_rounded_border)
        // Menggunakan warna teks primary (ungu)
        view.setTextColor(ContextCompat.getColor(this, R.color.mindease_primary))
    }
}