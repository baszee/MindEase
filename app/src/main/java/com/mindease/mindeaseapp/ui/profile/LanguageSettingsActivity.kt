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
import com.google.android.material.R as MaterialR // 🔥 Import alias untuk Material R

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

        // 🔥 FIX: Menggunakan string resource yang baru
        binding.toolbar.title = getString(R.string.select_language_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupLanguageOptions()
        highlightCurrentLanguage()
    }

    // 🔥 HELPER UNTUK MENDAPATKAN WARNA DARI THEME ATTRIBUTE
    @ColorInt
    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data // Menggunakan typedValue.data untuk warna tema
    }


    private fun setupLanguageOptions() {
        // 🔥 FIX 1: Menggunakan strings.xml baru
        binding.tvThemeLight.text = getString(R.string.language_english)
        binding.tvThemeDark.text = getString(R.string.language_indonesia)

        binding.tvThemeLight.setOnClickListener {
            setNewLocaleAndRestart(LANG_EN)
        }

        binding.tvThemeDark.setOnClickListener {
            setNewLocaleAndRestart(LANG_ID_ALT)
        }

        // Sembunyikan TextView lain di layout Themes
        // Asumsi: TextView header "TEMA WARNA CERAH" ada di atas tvColorGreentea
        if (binding.root.findViewById<View>(R.id.tv_theme_light)?.parent?.parent is android.widget.LinearLayout) {
            val parentLayout = binding.root.findViewById<View>(R.id.tv_theme_light).parent.parent as android.widget.LinearLayout
            if (parentLayout.getChildAt(0) is android.widget.TextView) {
                // Sembunyikan TextView header PILIHAN TEMA APLIKASI
                (parentLayout.getChildAt(0) as android.widget.TextView).visibility = View.GONE
            }
            if (parentLayout.getChildAt(4) is android.widget.TextView) {
                // Sembunyikan TextView header TEMA WARNA CERAH
                (parentLayout.getChildAt(4) as android.widget.TextView).visibility = View.GONE
            }
        }

        binding.tvColorGreentea.visibility = View.GONE
        binding.tvColorOceanblue.visibility = View.GONE
        binding.tvColorRosepink.visibility = View.GONE
        binding.tvColorAutumngold.visibility = View.GONE
    }

    private fun highlightCurrentLanguage() {
        val currentLang = ThemeManager.getLanguage(this)

        // 🔥 FIX 2: Menggunakan resolveThemeColor dengan referensi MaterialR
        val primaryColor = resolveThemeColor(MaterialR.attr.colorPrimary)
        val onSurfaceColor = resolveThemeColor(MaterialR.attr.colorOnSurface)
        val surfaceColor = resolveThemeColor(MaterialR.attr.colorSurface) // Warna background default tombol

        // Asumsi: Warna teks yang dipilih harus colorOnPrimary
        val selectedTextColor = resolveThemeColor(MaterialR.attr.colorOnPrimary)
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
    }


    private fun setNewLocaleAndRestart(languageCode: String) {
        ThemeManager.saveLanguage(this, languageCode)

        // Restart aplikasi dari ROOT activity
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)

        // Kill current process untuk force restart
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    // Fungsi updateResources yang asli, tidak digunakan karena attachBaseContext sudah handle
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration

        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}