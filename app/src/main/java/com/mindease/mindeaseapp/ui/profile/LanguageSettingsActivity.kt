package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityLanguageSettingsBinding
import com.mindease.mindeaseapp.utils.ThemeManager
import com.google.android.material.R as MaterialR
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.google.android.material.card.MaterialCardView

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSettingsBinding

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
        highlightCurrentLanguage()
    }

    @ColorInt
    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun setupListeners() {
        binding.cardEnglish.setOnClickListener {
            changeLanguage(LANG_EN)
        }

        binding.cardIndonesia.setOnClickListener {
            changeLanguage(LANG_ID)
        }
    }

    private fun changeLanguage(languageCode: String) {
        val currentLang = ThemeManager.getLanguageCode(this)

        if (currentLang == languageCode) {
            Toast.makeText(this, getString(R.string.language_already_selected), Toast.LENGTH_SHORT).show()
            return
        }

        // Simpan bahasa baru
        ThemeManager.saveLanguage(this, languageCode)

        // Tampilkan dialog konfirmasi
        showRestartDialog()
    }

    private fun highlightCurrentLanguage() {
        val currentLang = ThemeManager.getLanguageCode(this)

        val cardEn = binding.cardEnglish
        val cardId = binding.cardIndonesia

        val primaryColor = resolveThemeColor(MaterialR.attr.colorPrimary)

        fun applyHighlight(cardView: MaterialCardView) {
            cardView.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics
            ).toInt()
            cardView.strokeColor = primaryColor
        }

        fun resetHighlight(cardView: MaterialCardView) {
            cardView.strokeWidth = 0
            cardView.strokeColor = 0
        }

        if (currentLang == LANG_EN) {
            applyHighlight(cardEn)
            resetHighlight(cardId)
        } else if (currentLang == LANG_ID) {
            applyHighlight(cardId)
            resetHighlight(cardEn)
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm))
            .setMessage(getString(R.string.restart_app_prompt))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // ✅ KUNCI: Panggil restart dengan benar
                ThemeManager.restartApplication(this)
            }
            .show()
    }
}