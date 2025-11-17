package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivitySettingsBinding
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context

/**
 * Activity untuk halaman Pengaturan/Settings.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {

        // KATEGORI 1: TAMPILAN & FUNGSI

        // Navigasi ke Themes
        binding.tvThemes.setOnClickListener {
            val intent = Intent(this, ThemesActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Bahasa (BARU)
        binding.tvLanguage.setOnClickListener {
            val intent = Intent(this, LanguageSettingsActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Notifikasi (Placeholder)
        binding.tvNotifications.setOnClickListener {
            Toast.makeText(this, "Fitur Pengaturan Notifikasi (Dalam Pengembangan)", Toast.LENGTH_SHORT).show()
        }

        // KATEGORI 2: AKUN & KEAMANAN

        // Navigasi ke Change Password
        binding.tvChangePassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Delete Account
        binding.tvDeleteAccount.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java)
            startActivity(intent)
        }
    }
}