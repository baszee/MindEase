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

    // ✅ FIX UTAMA: Gunakan key LOWERCASING yang sesuai dengan ThemeManager.getThemeKey()
    private val themeViews by lazy {
        mapOf(
            "light" to binding.tvThemeLight,     // Dulu "INDIGO"
            "dark" to binding.tvThemeDark,       // Dulu "DARK_MODE"
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
        // ✅ FIX: Simpan key yang sudah benar (e.g., "dark")
        ThemeManager.saveTheme(this, key)
        ThemeManager.applyTheme(this) // Panggil ini untuk update Night Mode

        // ✅ FIX: Gunakan String Resources yang sudah diterjemahkan
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

        // 🔥 FIX UTAMA PROPAGATION: Restart aplikasi
        val intent = Intent(this, MainActivity::class.java).apply {
            // Hapus semua Activity yang ada di stack dan mulai MainActivity sebagai root baru
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        // PENTING: Tutup Activity Themes agar tidak kembali ke sini
        finish()
    }

    private fun highlightCurrentChoices() {
        // Karena key yang disimpan sekarang benar, highlight akan bekerja
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