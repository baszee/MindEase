package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivitySettingsBinding

/**
 * Activity untuk halaman Pengaturan/Settings.
 * Mengelola navigasi ke sub-menu seperti Edit Profile, Themes, Privacy Policy, dll.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {
        // Navigasi ke Edit Profile
        binding.tvEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Themes (Sudah dibuat di langkah sebelumnya)
        binding.tvThemes.setOnClickListener {
            val intent = Intent(this, ThemesActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Privacy Policy
        binding.tvPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke About App (Sudah dibuat di langkah sebelumnya)
        binding.tvAboutApp.setOnClickListener {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
        }

        // TODO: Tambahkan listener untuk Change Password dan opsi lainnya
        binding.tvChangePassword.setOnClickListener {
            // val intent = Intent(this, ChangePasswordActivity::class.java)
            // startActivity(intent)
            // Untuk saat ini bisa diabaikan atau tampilkan Toast jika Activity belum dibuat.
        }
    }
}