package com.mindease.mindeaseapp.ui.profile

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityAboutAppBinding

/**
 * Activity untuk halaman About App/Tentang Aplikasi.
 */
class AboutAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // FIX: Menggunakan fungsi getAppVersion yang menjamin pengembalian String non-nullable
        binding.tvAppVersion.text = "Version ${getAppVersion()}"

        // Tagline dari strings.xml
        binding.tvTagline.text = getString(R.string.placeholder_quote)

        Toast.makeText(this, "Informasi Aplikasi dimuat.", Toast.LENGTH_SHORT).show()
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