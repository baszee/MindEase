package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources // 🔥 FIX: Import yang hilang (Resources)
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityThemesBinding
import com.mindease.mindeaseapp.ui.home.MainActivity
import com.mindease.mindeaseapp.utils.ThemeManager
import java.util.Locale
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.google.android.material.R as MaterialR

/**
 * Activity untuk memilih pengaturan Bahasa.
 */
class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesBinding

    companion object {
        const val LANG_EN = "en"
        const val LANG_ID_ALT = "id"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        binding = ActivityThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup Toolbar dengan judul yang benar
        binding.toolbar.title = getString(R.string.select_language_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupLanguageOptions()
        highlightCurrentLanguage()
    }

    @ColorInt
    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }


    private fun setupLanguageOptions() {
        // 2. Gunakan string resource yang benar untuk opsi bahasa
        binding.tvThemeLight.text = getString(R.string.language_english)
        binding.tvThemeDark.text = getString(R.string.language_indonesia)

        binding.tvThemeLight.setOnClickListener {
            setNewLocaleAndRestart(LANG_EN)
        }

        binding.tvThemeDark.setOnClickListener {
            setNewLocaleAndRestart(LANG_ID_ALT)
        }

        // 3. FIX UI: Sembunyikan semua elemen layout tema yang tidak relevan
        binding.tvColorGreentea.visibility = View.GONE
        binding.tvColorOceanblue.visibility = View.GONE
        binding.tvColorRosepink.visibility = View.GONE
        binding.tvColorAutumngold.visibility = View.GONE

        // Logika untuk menyembunyikan header "PILIHAN TEMA APLIKASI", separator, dan "TEMA WARNA CERAH"
        val parentLayout = binding.tvThemeLight.parent.parent as? android.widget.LinearLayout
        parentLayout?.let {
            // Sembunyikan header "PILIHAN TEMA APLIKASI" (Child 0)
            if (it.childCount > 0 && it.getChildAt(0) is TextView) {
                (it.getChildAt(0) as TextView).visibility = View.GONE
            }

            // Sembunyikan View separator dan header "TEMA WARNA CERAH"
            // Kita mulai dari index 3 (setelah 2 opsi bahasa) dan sembunyikan sisanya.
            for (i in 3 until it.childCount) {
                it.getChildAt(i)?.visibility = View.GONE
            }
        }
    }

    private fun highlightCurrentLanguage() {
        val currentLang = ThemeManager.getLanguage(this)

        val primaryColor = resolveThemeColor(MaterialR.attr.colorPrimary)
        val selectedTextColor = resolveThemeColor(MaterialR.attr.colorOnPrimary)
        val defaultBgColor = resolveThemeColor(MaterialR.attr.colorSurface)
        val defaultTextColor = resolveThemeColor(MaterialR.attr.colorOnSurface)

        fun applyHighlight(view: TextView) {
            view.setBackgroundColor(primaryColor)
            view.setTextColor(selectedTextColor)
        }

        fun resetHighlight(view: TextView) {
            // Menggunakan warna latar belakang Surface
            view.setBackgroundColor(defaultBgColor)
            view.setTextColor(defaultTextColor)
        }

        if (currentLang == LANG_EN) {
            applyHighlight(binding.tvThemeLight)
            resetHighlight(binding.tvThemeDark)
        } else if (currentLang == LANG_ID_ALT) {
            applyHighlight(binding.tvThemeDark)
            resetHighlight(binding.tvThemeLight)
        }
    }


    private fun setNewLocaleAndRestart(languageCode: String) {
        ThemeManager.saveLanguage(this, languageCode)
        Toast.makeText(this, "Bahasa diubah. Memuat ulang aplikasi...", Toast.LENGTH_SHORT).show()

        // Navigasi ke MainActivity sebagai root baru
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    // Fungsi ini dipertahankan agar tidak terjadi Unresolved reference
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}