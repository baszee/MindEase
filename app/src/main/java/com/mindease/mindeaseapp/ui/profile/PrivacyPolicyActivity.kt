package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivityPrivacyPolicyBinding
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding
    private lateinit var remoteConfig: FirebaseRemoteConfig

    companion object {
        private const val PRIVACY_POLICY_KEY = "privacy_policy_text"
        private const val DEFAULT_POLICY_TEXT =
            "Kebijakan Privasi tidak dapat dimuat. Mohon cek koneksi internet Anda atau coba lagi nanti."
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
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
            minimumFetchIntervalInSeconds = 0L
        }

        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.setDefaultsAsync(
            mapOf(
                PRIVACY_POLICY_KEY to DEFAULT_POLICY_TEXT
            )
        )
    }

    /**
     * Mengambil teks kebijakan dari Remote Config dan menampilkan.
     */
    private fun fetchAndDisplayPolicy() {

        // Tampilkan versi default/cache dulu
        binding.tvContent.text =
            formatText(remoteConfig.getString(PRIVACY_POLICY_KEY))

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updatedText = remoteConfig.getString(PRIVACY_POLICY_KEY)
                    binding.tvContent.text = formatText(updatedText)
                    Toast.makeText(
                        this,
                        "Kebijakan Privasi berhasil diperbarui.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Gagal memuat kebijakan, menampilkan versi lokal.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Mengubah delimiter "|||"" menjadi newline agar tampil rapi.
     */
    private fun formatText(raw: String): String {
        return raw.replace("|||", "\n\n")
    }
}
