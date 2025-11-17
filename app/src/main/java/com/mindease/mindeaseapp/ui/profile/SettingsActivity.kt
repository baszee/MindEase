package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivitySettingsBinding
import com.mindease.mindeaseapp.utils.ThemeManager // 🔥 Wajib import

/**
 * Activity untuk halaman Pengaturan/Settings.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 FIX KRITIS: Terapkan tema penuh SEBELUM super.onCreate()
        setTheme(ThemeManager.getThemeStyleResId(this))

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {
        // KATEGORI 1: AKUN & KEAMANAN

        // Navigasi ke Edit Profile (Sekarang mencakup Bio/Gambar Profil)
        binding.tvEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

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

        // KATEGORI 2: TAMPILAN & FUNGSI

        // Navigasi ke Themes
        binding.tvThemes.setOnClickListener {
            val intent = Intent(this, ThemesActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Bahasa (Placeholder untuk fitur mendatang)
        binding.tvLanguage.setOnClickListener {
            // TODO: Implement Language Settings Activity
            Toast.makeText(this, "Fitur Pengaturan Bahasa (Dalam Pengembangan)", Toast.LENGTH_SHORT).show()
        }

        // Navigasi ke Notifikasi (Placeholder untuk fitur mendatang)
        binding.tvNotifications.setOnClickListener {
            // TODO: Implement Notification Settings Activity
            Toast.makeText(this, "Fitur Pengaturan Notifikasi (Dalam Pengembangan)", Toast.LENGTH_SHORT).show()
        }

        // KATEGORI 3: LAINNYA & LEGAL

        // Navigasi ke Syarat & Ketentuan (Placeholder)
        binding.tvTermsAndConditions.setOnClickListener {
            // TODO: Implement Terms and Conditions Activity
            Toast.makeText(this, "Fitur Syarat & Ketentuan (Dalam Pengembangan)", Toast.LENGTH_SHORT).show()
        }

        // Navigasi ke Privacy Policy
        binding.tvPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke About App
        binding.tvAboutApp.setOnClickListener {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
        }
    }
}