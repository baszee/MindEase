package com.mindease.mindeaseapp.ui.profile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityNotificationSettingsBinding
import com.mindease.mindeaseapp.utils.NotificationPreference
import com.mindease.mindeaseapp.utils.ThemeManager

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding

    // Menggunakan ThemeManager untuk tema
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Terapkan tema sebelum memanggil super.onCreate
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)

        // Inisialisasi binding
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar kembali
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadSettings()
        setupListeners()
    }

    /**
     * Memuat status notifikasi yang tersimpan secara lokal dan mengatur status Switch.
     */
    private fun loadSettings() {
        // Mendapatkan status dari penyimpanan lokal (SharedPreferences)
        val isEnabled = NotificationPreference.getNotificationStatus(this)

        // Atur status Switch. Setting ini tidak akan memicu listener karena diatur secara programatis.
        binding.switchNotifications.isChecked = isEnabled
    }

    /**
     * Menyiapkan listener untuk Switch notifikasi.
     */
    private fun setupListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting(isChecked)
        }
    }

    /**
     * Menyimpan status notifikasi ke SharedPreferences (storage lokal).
     */
    private fun saveNotificationSetting(isEnabled: Boolean) {
        NotificationPreference.saveNotificationStatus(this, isEnabled)
        Toast.makeText(this, getString(R.string.settings_saved_successfully), Toast.LENGTH_SHORT).show()

        // TODO: Anda perlu menambahkan logika untuk menjadwalkan atau membatalkan
        // alarm/work manager notifikasi di sini berdasarkan nilai 'isEnabled'.
    }
}