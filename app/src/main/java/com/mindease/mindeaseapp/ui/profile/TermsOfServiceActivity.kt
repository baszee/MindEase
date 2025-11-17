package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivityTermsOfServiceBinding
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
import com.mindease.mindeaseapp.R

class TermsOfServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsOfServiceBinding
    private lateinit var remoteConfig: FirebaseRemoteConfig

    companion object {
        // Kunci Remote Config baru untuk Ketentuan Layanan
        private const val TERMS_OF_SERVICE_KEY = "terms_of_service_text"
        private const val DEFAULT_TERMS_TEXT = "Ketentuan Layanan tidak dapat dimuat. Mohon cek koneksi internet Anda atau coba lagi nanti."
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        // Asumsi ActivityTermsOfServiceBinding telah dibuat
        binding = ActivityTermsOfServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.title = getString(R.string.terms_of_service)

        setupRemoteConfig()
        fetchAndDisplayTerms()
    }

    /**
     * Menginisialisasi Firebase Remote Config dan menetapkan nilai default lokal.
     */
    private fun setupRemoteConfig() {
        remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            // Set interval fetch ke 0 untuk pengujian/debug (di production sebaiknya 3600)
            minimumFetchIntervalInSeconds = 0L
        }

        remoteConfig.setConfigSettingsAsync(configSettings)

        // Menggunakan kunci yang baru
        remoteConfig.setDefaultsAsync(mapOf(
            TERMS_OF_SERVICE_KEY to DEFAULT_TERMS_TEXT
        ))
    }

    private fun formatText(raw: String): String {
        return raw.replace("|||", "\n\n")
    }
    /**
     * Mengambil teks ketentuan dari Remote Config dan menampilkan.
     */
    private fun fetchAndDisplayTerms() {
        // Tampilkan teks yang saat ini tersedia (default lokal atau cache) saat loading
        binding.tvContent.text = formatText(remoteConfig.getString(TERMS_OF_SERVICE_KEY))

        // Lakukan fetch dari cloud (fetchAndActivate)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jika sukses, aktifkan nilai baru dan update UI
                    val updatedText = remoteConfig.getString(TERMS_OF_SERVICE_KEY)
                    binding.tvContent.text = formatText(updatedText)
                    Toast.makeText(this, "Ketentuan Layanan berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                } else {
                    // Jika gagal, Toast error tapi tetap menampilkan nilai default/cache
                    Toast.makeText(this, "Gagal memuat ketentuan, menampilkan versi lokal.", Toast.LENGTH_LONG).show()
                }
            }
    }
}