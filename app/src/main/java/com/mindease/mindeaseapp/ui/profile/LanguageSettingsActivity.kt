package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityLanguageSettingsBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.ThemeManager
import com.google.android.material.R as MaterialR
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.google.android.material.card.MaterialCardView

/**
 * Activity untuk memilih pengaturan Bahasa.
 */
class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSettingsBinding

    // ✅ FIX SINTAKSIS: Menghapus duplikasi 'val'
    companion object {
        const val LANG_EN = "en"
        const val LANG_ID = "id"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        binding = ActivityLanguageSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupListeners()
        // Panggil highlight setiap onCreate/re-create
        highlightCurrentLanguage()
    }

    // Helper untuk mengambil warna tema
    @ColorInt
    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun setupListeners() {
        // Navigasi ke Bahasa Inggris - sekarang klik MaterialCardView
        binding.cardEnglish.setOnClickListener {
            setNewLocaleAndRestart(LANG_EN)
        }

        // Navigasi ke Bahasa Indonesia - sekarang klik MaterialCardView
        binding.cardIndonesia.setOnClickListener {
            setNewLocaleAndRestart(LANG_ID)
        }
    }

    private fun highlightCurrentLanguage() {
        val currentLang = ThemeManager.getLanguageCode(this)

        // ✅ FIX: Menggunakan ID CardView yang benar
        val cardEn = binding.cardEnglish
        val cardId = binding.cardIndonesia

        // Ambil warna tema
        val primaryColor = resolveThemeColor(MaterialR.attr.colorPrimary)

        fun applyHighlight(cardView: MaterialCardView) {
            // Memberi warna stroke (garis pinggir) Primary
            cardView.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt() // Tebal stroke 2dp
            cardView.strokeColor = primaryColor
        }

        fun resetHighlight(cardView: MaterialCardView) {
            // Menghapus stroke
            cardView.strokeWidth = 0
            cardView.strokeColor = 0
        }

        // Tentukan mana yang harus di-highlight
        if (currentLang == LANG_EN) {
            applyHighlight(cardEn)
            resetHighlight(cardId)
        } else if (currentLang == LANG_ID) {
            applyHighlight(cardId)
            resetHighlight(cardEn)
        }
    }


    private fun setNewLocaleAndRestart(languageCode: String) {
        // Hanya restart jika pilihan bahasa berbeda dari yang sekarang
        if (ThemeManager.getLanguageCode(this) != languageCode) {
            ThemeManager.saveLanguage(this, languageCode)
            showRestartDialog()
        } else {
            // Asumsi string 'language_already_selected' ada
            Toast.makeText(this, getString(R.string.language_already_selected), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm))
            .setMessage(getString(R.string.restart_app_prompt))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                restartApp()
            }
            .show()
    }

    private fun restartApp() {
        // Melakukan full restart untuk menerapkan bahasa
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}