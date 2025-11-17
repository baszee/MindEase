package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityAboutAppBinding
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context

/**
 * Activity untuk halaman About App/Tentang Aplikasi, yang kini berfungsi sebagai hub untuk Legal & Info.
 */
class AboutAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutAppBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        binding = ActivityAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupNavigationListeners()
    }

    private fun setupNavigationListeners() {
        // Navigasi ke Syarat & Ketentuan (PERUBAHAN INI)
        binding.tvTermsAndConditions.setOnClickListener {
            val intent = Intent(this, TermsOfServiceActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Kebijakan Privasi
        binding.tvPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        // Navigasi ke Info Versi Aplikasi (Saat ini hanya menampilkan toast versi)
        binding.tvAppInfo.setOnClickListener {
            Toast.makeText(this, "MindEase Version: ${getAppVersion()}", Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Mengambil nomor versi aplikasi dari PackageManager, menjamin non-nullable String.
     */
    private fun getAppVersion(): String {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            // FIX: Menggunakan Elvis operator untuk menangani String? (nullable)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "N/A"
        }
    }
}