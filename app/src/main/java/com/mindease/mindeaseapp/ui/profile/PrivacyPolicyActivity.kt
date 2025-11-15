package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivityPrivacyPolicyBinding

// Import Firebase yang diperlukan
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

// Catatan: Referensi ke BuildConfig dihapus untuk menghindari Unresolved reference error.

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding
    private lateinit var remoteConfig: FirebaseRemoteConfig

    companion object {
        private const val PRIVACY_POLICY_KEY = "privacy_policy_text"
        private const val DEFAULT_POLICY_TEXT = "Kebijakan Privasi tidak dapat dimuat. Mohon cek koneksi internet Anda atau coba lagi nanti."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupRemoteConfig()
        fetchAndDisplayPolicy()
    }

    /**
     * Menginisialisasi Firebase Remote Config dan menetapkan nilai default lokal.
     */
    private fun setupRemoteConfig() {
        remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            // FIX: Mengganti BuildConfig.DEBUG dengan nilai hardcoded 0 untuk testing.
            // Di produksi (production), nilai ini harus 3600 (1 jam).
            minimumFetchIntervalInSeconds = 0L
        }

        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.setDefaultsAsync(mapOf(
            PRIVACY_POLICY_KEY to DEFAULT_POLICY_TEXT
        ))
    }

    /**
     * Mengambil teks kebijakan dari Remote Config dan menampilkan.
     */
    private fun fetchAndDisplayPolicy() {
        // Tampilkan teks yang saat ini tersedia (default lokal atau cache) saat loading
        binding.tvContent.text = remoteConfig.getString(PRIVACY_POLICY_KEY)

        // Lakukan fetch dari cloud (fetchAndActivate)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Jika sukses, aktifkan nilai baru dan update UI
                    val updatedText = remoteConfig.getString(PRIVACY_POLICY_KEY)
                    binding.tvContent.text = updatedText
                    Toast.makeText(this, "Kebijakan Privasi berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                } else {
                    // Jika gagal, Toast error tapi tetap menampilkan nilai default/cache
                    Toast.makeText(this, "Gagal memuat kebijakan, menampilkan versi lokal.", Toast.LENGTH_LONG).show()
                }
            }
    }
}